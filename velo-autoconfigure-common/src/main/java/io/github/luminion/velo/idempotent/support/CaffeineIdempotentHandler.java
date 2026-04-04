package io.github.luminion.velo.idempotent.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.github.luminion.velo.idempotent.IdempotentHandler;

import java.util.concurrent.TimeUnit;

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
    private final Cache<String, Marker> cache = Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfter(new Expiry<String, Marker>() {
                @Override
                public long expireAfterCreate(String key, Marker marker, long currentTime) {
                    return marker.ttlNanos();
                }
                @Override
                public long expireAfterUpdate(String key, Marker marker, long currentTime, long currentDuration) {
                    return currentDuration;
                }
                @Override
                public long expireAfterRead(String key, Marker marker, long currentTime, long currentDuration) {
                    return currentDuration;
                }
            })
            .build();

    @Override
    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        Marker existing = cache.asMap().putIfAbsent(key, new Marker(unit.toNanos(timeout)));
        return existing == null;
    }

    @Override
    public void unlock(String key) {
        cache.invalidate(key);
    }

    private static final class Marker {
        private final long ttlNanos;

        private Marker(long ttlNanos) {
            this.ttlNanos = ttlNanos;
        }

        private long ttlNanos() {
            return ttlNanos;
        }
    }
}
