package io.github.luminion.velo.lock.support;

import io.github.luminion.velo.lock.LockHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的分布式锁实现。
 *
 * 每次成功加锁都生成新的 owner token，并按线程保存，避免旧请求在锁过期后误删新锁。
 */
@Slf4j
@RequiredArgsConstructor
public class RedisLockHandler implements LockHandler {

    private static final String RELEASE_LUA = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else return 0 end";

    private final StringRedisTemplate redisTemplate;
    private final ThreadLocal<Map<String, String>> lockValues = ThreadLocal.withInitial(HashMap::new);

    @Override
    public boolean lock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        long waitMillis = unit.toMillis(waitTime);
        long leaseMillis = unit.toMillis(leaseTime);
        long start = System.currentTimeMillis();

        do {
            String lockValue = UUID.randomUUID().toString();
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, lockValue, leaseMillis, TimeUnit.MILLISECONDS);
            if (Boolean.TRUE.equals(success)) {
                // 每次加锁都记录一份新的 token，解锁时用它和 Redis 当前值比对，避免误删已续租或被他人重建的锁。
                lockValues.get().put(key, lockValue);
                return true;
            }

            if (waitMillis <= 0) {
                break;
            }

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
        Map<String, String> values = lockValues.get();
        String lockValue = values.remove(key);
        if (lockValue == null) {
            if (values.isEmpty()) {
                lockValues.remove();
            }
            return;
        }

        try {
            // 删除动作必须和 token 校验放在同一个 Lua 脚本里，才能保证“检查后删除”是原子的。
            redisTemplate.execute(new DefaultRedisScript<>(RELEASE_LUA, Long.class), Collections.singletonList(key), lockValue);
        } catch (Exception e) {
            log.warn("Redis unlock failed for key: {}", key, e);
        } finally {
            if (values.isEmpty()) {
                lockValues.remove();
            }
        }
    }
}
