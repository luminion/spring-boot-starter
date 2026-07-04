package io.github.luminion.velo.lock.support;

import io.github.luminion.velo.lock.LockHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 Redis 的分布式锁实现。
 *
 * 每次成功加锁都生成新的 owner token，并按线程以栈结构保存，
 * 避免同线程同 key 嵌套加锁时 token 被覆盖导致误删。
 */
@Slf4j
@RequiredArgsConstructor
public class RedisLockHandler implements LockHandler {

    private static final String RELEASE_LUA = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else return 0 end";

    // 脚本无状态，提取为静态常量复用，避免每次 unlock 重复构建
    private static final RedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(RELEASE_LUA, Long.class);

    // 看门狗降级后的默认租约，与 @Lock 注解默认 lease(30s) 保持一致
    private static final long WATCHDOG_FALLBACK_LEASE_MILLIS = TimeUnit.SECONDS.toMillis(30);

    private final StringRedisTemplate redisTemplate;
    private final ThreadLocal<Map<String, Deque<String>>> lockValues = ThreadLocal.withInitial(HashMap::new);
    // 看门狗降级告警只打一次，避免每次加锁刷屏
    private final AtomicBoolean watchdogFallbackWarned = new AtomicBoolean(false);

    @Override
    public boolean lock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        long waitMillis = unit.toMillis(waitTime);
        // 该后端基于 setIfAbsent 固定 TTL，无续约线程，不支持看门狗(-1)；
        // 降级为固定租约避免负 TTL 直接报错，并提示改用 Redisson 以获得自动续约。
        long leaseMillis;
        if (leaseTime < 0L) {
            leaseMillis = WATCHDOG_FALLBACK_LEASE_MILLIS;
            if (watchdogFallbackWarned.compareAndSet(false, true)) {
                log.warn("[Velo Starter] RedisLockHandler does not support watchdog auto-renewal (lease=-1). " +
                        "Falling back to a fixed lease of {}ms. If your business method may run longer, " +
                        "set an explicit lease or switch to RedissonLockHandler.", WATCHDOG_FALLBACK_LEASE_MILLIS);
            }
        } else {
            leaseMillis = unit.toMillis(leaseTime);
        }
        long start = System.currentTimeMillis();

        do {
            String lockValue = UUID.randomUUID().toString();
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, lockValue, leaseMillis, TimeUnit.MILLISECONDS);
            if (success == null) {
                // setIfAbsent 返回 null 说明命令被 Redis 事务/pipeline 排队而非立即执行，加锁判定失效。
                // 分布式锁不应包裹在 Redis 事务里，打 WARN 提示误用。
                log.warn("[Velo Starter] Redis lock setIfAbsent returned null for key '{}'. " +
                        "This usually means the operation is wrapped in a Redis transaction/pipeline, " +
                        "which defers execution and breaks locking. Avoid acquiring locks inside a Redis transaction.", key);
            }
            if (Boolean.TRUE.equals(success)) {
                // 每次加锁都记录一份新的 token，解锁时用它和 Redis 当前值比对，避免误删已续租或被他人重建的锁。
                // 使用栈结构存储，支持同线程同 key 的嵌套加锁场景。
                lockValues.get().computeIfAbsent(key, k -> new ArrayDeque<>()).push(lockValue);
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
        Map<String, Deque<String>> values = lockValues.get();
        Deque<String> stack = values.get(key);
        if (stack == null || stack.isEmpty()) {
            if (values.isEmpty()) {
                lockValues.remove();
            }
            return;
        }

        String lockValue = stack.pop();
        if (stack.isEmpty()) {
            values.remove(key);
        }

        try {
            // 删除动作必须和 token 校验放在同一个 Lua 脚本里，才能保证“检查后删除”是原子的。
            redisTemplate.execute(RELEASE_SCRIPT, Collections.singletonList(key), lockValue);
        } catch (Exception e) {
            log.warn("Redis unlock failed for key: {}", key, e);
        } finally {
            if (values.isEmpty()) {
                lockValues.remove();
            }
        }
    }
}
