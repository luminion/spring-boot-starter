package io.github.luminion.starter.idempotent.support;

import io.github.luminion.starter.idempotent.IdempotentHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 JDK ConcurrentHashMap 的本地幂等处理器 (兜底方案)
 * 使用 compute 原子方法保证并发安全性
 *
 * @author luminion
 * @since 1.0.0
 */
public class JdkIdempotentHandler implements IdempotentHandler {

    private final ConcurrentHashMap<String, Long> lockMap = new ConcurrentHashMap<>();
    // 清理标志位，保证同一时间只有一个线程在做清理工作
    private final AtomicBoolean isCleaning = new AtomicBoolean(false);

    @Override
    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        long now = System.currentTimeMillis();
        long expireAt = now + unit.toMillis(timeout);
        AtomicBoolean success = new AtomicBoolean(false);
        lockMap.compute(key, (k, v) -> {
            if (v == null || v <= now) {
                success.set(true);
                return expireAt;
            }
            return v;
        });

        // 异步且安全的清理机制
        if (lockMap.mappingCount() > 1024 && isCleaning.compareAndSet(false, true)) {
            CompletableFuture.runAsync(() -> {
                try {
                    long currentTime = System.currentTimeMillis();
                    lockMap.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
                } finally {
                    isCleaning.set(false); // 清理完毕，释放标志位
                }
            });
        }
        return success.get();
    }
}