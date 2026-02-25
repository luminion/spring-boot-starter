package io.github.luminion.starter.feature.idempotent.support;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.luminion.starter.feature.idempotent.IdempotentHandler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 Guava Cache 的本地幂等处理器
 * 通过存储过期时间戳并利用 asMap().compute 方法保证原子性
 *
 * @author luminion
 * @since 1.0.1
 */
public class GuavaIdempotentHandler implements IdempotentHandler {

    /**
     * 容器设置兜底过期，防止内存泄漏
     * 具体的幂等时效由 Value (时间戳) 控制
     */
    private final Cache<String, Long> locks = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    @Override
    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        long now = System.currentTimeMillis();
        long expireAt = now + unit.toMillis(timeout);
        AtomicBoolean success = new AtomicBoolean(false);

        locks.asMap().compute(key, (k, v) -> {
            if (v == null || v <= now) {
                success.set(true);
                return expireAt;
            }
            return v;
        });

        return success.get();
    }

    @Override
    public void release(String key) {
        locks.invalidate(key);
    }
}
