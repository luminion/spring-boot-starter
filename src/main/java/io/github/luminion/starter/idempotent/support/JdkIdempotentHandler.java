package io.github.luminion.starter.idempotent.support;

import io.github.luminion.starter.idempotent.IdempotentHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 JDK ConcurrentHashMap 的本地幂等处理器 (兜底方案)
 * 使用 compute 原子方法保证并发安全性
 *
 * @author luminion
 * @since 1.0.1
 */
public class JdkIdempotentHandler implements IdempotentHandler {

    /**
     * Key: 幂等键
     * Value: 过期时间戳 (毫秒)
     */
    private final ConcurrentHashMap<String, Long> lockMap = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        long now = System.currentTimeMillis();
        long expireAt = now + unit.toMillis(timeout);
        AtomicBoolean success = new AtomicBoolean(false);

        // 使用 compute 保证原子性：读取 -> 判断 -> 写入 全过程加锁（锁在 HashEntry 上）
        lockMap.compute(key, (k, v) -> {
            if (v == null || v <= now) {
                success.set(true);
                return expireAt;
            }
            return v;
        });

        if (success.get()) {
            // 顺便清理一下过期的键 (每隔一段时间或触发阈值)
            cleanUp();
        }

        return success.get();
    }

    @Override
    public void release(String key) {
        lockMap.remove(key);
    }

    private void cleanUp() {
        // 定期清理策略：当容量超过 1024 时执行全量清理
        if (lockMap.size() > 1024) {
            long now = System.currentTimeMillis();
            lockMap.entrySet().removeIf(entry -> entry.getValue() <= now);
        }
    }
}
