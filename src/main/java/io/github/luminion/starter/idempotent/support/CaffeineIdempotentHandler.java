package io.github.luminion.starter.idempotent.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.github.luminion.starter.idempotent.IdempotentHandler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 Caffeine 的本地幂等处理器
 * 通过存储过期时间戳并利用 asMap().compute 方法保证原子性
 *
 * @author luminion
 */
public class CaffeineIdempotentHandler implements IdempotentHandler {

    /**
     * 容器设置兜底过期，防止内存泄漏
     * 具体的幂等时效由 Value (时间戳) 控制
     */
    // 核心优化：使用自定义过期策略 (Expiry) 来动态控制每个 Key 的存活时间
    private final Cache<String, Long> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfter(new Expiry<String, Long>() {
                @Override
                public long expireAfterCreate(String key, Long timeoutNanos, long currentTime) {
                    return timeoutNanos; // 创建时，直接把传进来的时间作为存活时间
                }
                @Override
                public long expireAfterUpdate(String k, Long v, long t, long currentDuration) {
                    return currentDuration;
                }
                @Override
                public long expireAfterRead(String k, Long v, long t, long currentDuration) {
                    return currentDuration;
                }
            })
            .build();

    @Override
    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        long timeoutNanos = unit.toNanos(timeout);
        // 如果 absent，返回 null 并放入 timeoutNanos。如果已存在，返回旧值。
        Long existing = cache.asMap().putIfAbsent(key, timeoutNanos);
        // existing == null 说明之前没有，加锁（防重）成功
        return existing == null;
    }
    
}
