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
 * <p>支持同线程可重入：按线程以栈结构记录持有情况，同一线程对同一 key 再次加锁时只在本地增加持有计数、
 * 不再访问 Redis，避免自己等待自己导致的死锁；只有最外层 unlock 才真正删除 Redis 锁。
 *
 * <p>重入不刷新 Redis TTL——续约由最外层首次设置的 lease 兜底。若重入链整体执行时间可能超过 lease，
 * 需显式调大 lease 或改用 Redisson 看门狗。
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
        Deque<String> stack = lockValues.get().computeIfAbsent(key, k -> new ArrayDeque<>());
        // 同线程已持有该 key：本地重入，仅增加持有计数(压入同一 owner token)，不再访问 Redis
        if (!stack.isEmpty()) {
            stack.push(stack.peek());
            return true;
        }

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
                // 首次获取成功，记录 owner token，解锁时用它和 Redis 当前值比对，避免误删已续租或被他人重建的锁。
                stack.push(lockValue);
                return true;
            }

            if (waitMillis <= 0) {
                break;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cleanupIfEmpty(key, stack);
                return false;
            }
        } while (System.currentTimeMillis() - start < waitMillis);

        // 首次获取失败，清理刚创建的空栈，避免 ThreadLocal 中残留空条目
        cleanupIfEmpty(key, stack);
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
        boolean outermost = stack.isEmpty();
        if (outermost) {
            values.remove(key);
        }

        try {
            // 仅最外层释放时才真正删除 Redis 锁；重入的内层释放只递减本地持有计数。
            // 删除动作必须和 token 校验放在同一个 Lua 脚本里，才能保证"检查后删除"是原子的。
            if (outermost) {
                redisTemplate.execute(RELEASE_SCRIPT, Collections.singletonList(key), lockValue);
            }
        } catch (Exception e) {
            log.warn("Redis unlock failed for key: {}", key, e);
        } finally {
            if (values.isEmpty()) {
                lockValues.remove();
            }
        }
    }

    private void cleanupIfEmpty(String key, Deque<String> stack) {
        if (stack.isEmpty()) {
            Map<String, Deque<String>> values = lockValues.get();
            values.remove(key);
            if (values.isEmpty()) {
                lockValues.remove();
            }
        }
    }
}
