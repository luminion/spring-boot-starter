package io.github.luminion.autoconfigure.aop.spi.limiter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.luminion.autoconfigure.aop.annotation.RateLimit;
import io.github.luminion.autoconfigure.aop.spi.RateLimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于Caffeine缓存的固定时间窗口限流
 * <p><b>注意:</b> 使用此实现需要添加 Caffeine 依赖 (com.github.ben-manes.caffeine:caffeine)。
 * @author luminion
 */
public class CaffeineRateLimiter implements RateLimiter {

    private final ConcurrentMap<Integer, Cache<String, AtomicLong>> caches = new ConcurrentHashMap<>();

    @Override
    public boolean doLimit(String signature, RateLimit rateLimit) {
        int seconds = rateLimit.seconds();
        int count = rateLimit.count();

        // 根据不同的过期时间, 获取或创建一个专用的Caffeine Cache实例
        Cache<String, AtomicLong> cache = caches.computeIfAbsent(seconds, sec ->
                Caffeine.newBuilder()
                        .expireAfterWrite(sec, TimeUnit.SECONDS)
                        .build()
        );

        // 从缓存中获取计数器, 如果不存在则初始化为0
        AtomicLong counter = cache.get(signature, k -> new AtomicLong(0));

        // 计数值加1, 并判断是否超过限制
        return counter.incrementAndGet() <= count;
    }
}
