package io.github.luminion.starter.support.ratelimit.support;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.github.luminion.starter.support.ratelimit.annotation.RateLimit;
import io.github.luminion.starter.support.ratelimit.spi.RateLimiter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 基于Guava的令牌桶算法限流处理器。
 * <p>此实现使用Guava的Cache来自动管理内存，并支持动态调整速率。
 * <p><b>依赖:</b> {@code com.google.guava:guava}
 *
 * @author luminion
 */
@SuppressWarnings("UnstableApiUsage")
public class GuavaRateLimiter implements RateLimiter {

    // 使用Guava的LoadingCache来存储和管理RateLimiter实例
    // 当一个限流器在10分钟内没有被访问时，它将被自动回收，从而防止内存泄漏。
    private final LoadingCache<String, com.google.common.util.concurrent.RateLimiter> limiters = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<String, com.google.common.util.concurrent.RateLimiter>() {
                @Override
                public com.google.common.util.concurrent.RateLimiter load(String key) {
                    // 注意：这里的key是signature，但创建时我们无法直接获得rateLimit注解，
                    // 所以我们创建一个占位符限流器，速率将在后面动态调整。
                    return com.google.common.util.concurrent.RateLimiter.create(1.0);
                }
            });

    @Override
    public boolean tryAccess(String signature, RateLimit rateLimit) {
        try {
            double permitsPerSecond = (double) rateLimit.count() / rateLimit.seconds();
            
            // 从缓存中获取限流器
            com.google.common.util.concurrent.RateLimiter limiter = limiters.get(signature);

            // 检查并动态更新速率，以支持配置的热修改
            if (limiter.getRate() != permitsPerSecond) {
                limiter.setRate(permitsPerSecond);
            }

            // 尝试获取令牌
            return limiter.tryAcquire();
        } catch (ExecutionException e) {
            // 在CacheLoader中我们没有声明任何受检异常，所以这里理论上不会发生。
            // 但为了健壮性，我们处理它并默认拒绝访问。
            return false;
        }
    }
}
