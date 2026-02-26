package io.github.luminion.starter.lock.support;

import io.github.luminion.starter.lock.LockHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的分布式锁实现 (Lua 脚本保证原子性)
 * 暂不支持重入，适用于大多数简单业务场景
 *
 * @author luminion
 */
@Slf4j
@RequiredArgsConstructor
public class RedisLockHandler implements LockHandler {

    private final StringRedisTemplate redisTemplate;
    private final String lockValue = UUID.randomUUID().toString();

    // 释放锁的 Lua 脚本：只有当锁的值匹配时才删除
    private static final String RELEASE_LUA = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else return 0 end";

    @Override
    public boolean lock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        long waitMillis = unit.toMillis(waitTime);
        long leaseMillis = unit.toMillis(leaseTime);
        long start = System.currentTimeMillis();

        do {
            // SET key value NX PX leaseTime
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, lockValue, leaseMillis,
                    TimeUnit.MILLISECONDS);
            if (Boolean.TRUE.equals(success)) {
                return true;
            }

            if (waitMillis <= 0)
                break;

            // 简单的自旋重试
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        } while (System.currentTimeMillis() - start < waitMillis);

        return false;
    }

    @Override
    public void unlock(String key) {
        try {
            redisTemplate.execute(new DefaultRedisScript<>(RELEASE_LUA, Long.class),
                    Collections.singletonList(key), lockValue);
        } catch (Exception e) {
            log.warn("Redis unlock failed for key: {}", key, e);
        }
    }
}
