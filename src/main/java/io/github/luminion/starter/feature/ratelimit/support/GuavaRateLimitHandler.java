package io.github.luminion.starter.feature.ratelimit.support;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import io.github.luminion.starter.feature.ratelimit.RateLimitHandler;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Guava 的本地限流器
 *
 * @author luminion
 * @since 1.0.1
 */
public class GuavaRateLimitHandler implements RateLimitHandler {

    /**
     * 注意：Guava 的 RateLimitHandler 是针对单 Key 的对象。
     * 这里我们缓存这些对象。
     */
    private final Cache<String, RateLimiter> limiters = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    @Override
    public boolean tryAcquire(String key, double rate) {
        try {
            RateLimiter limiter = limiters.get(key, () -> RateLimiter.create(rate));
            // 如果运行中速率发生变化，动态更新
            if (limiter.getRate() != rate) {
                limiter.setRate(rate);
            }

            return limiter.tryAcquire();
        } catch (ExecutionException e) {
            return false;
        }
    }
}
