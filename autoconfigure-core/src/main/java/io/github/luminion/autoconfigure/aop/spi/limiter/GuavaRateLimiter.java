package io.github.luminion.autoconfigure.aop.spi.limiter;

import io.github.luminion.autoconfigure.aop.annotation.RateLimit;
import io.github.luminion.autoconfigure.aop.spi.RateLimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Guava令牌桶算法限流
 * <p><b>注意:</b> 使用此实现需要添加 Guava 依赖 (com.google.guava:guava)。
 * @author luminion
 */
@SuppressWarnings("UnstableApiUsage")
public class GuavaRateLimiter implements RateLimiter {

    private final ConcurrentMap<String, com.google.common.util.concurrent.RateLimiter> limiters = new ConcurrentHashMap<>();

    @Override
    public boolean doLimit(String signature, RateLimit rateLimit) {
        double permitsPerSecond = (double) rateLimit.count() / rateLimit.seconds();

        // 获取或创建限流器
        // 如果注解中的速率发生变化, 此实现可以动态调整它
        com.google.common.util.concurrent.RateLimiter limiter = limiters.computeIfAbsent(signature, 
                k -> com.google.common.util.concurrent.RateLimiter.create(permitsPerSecond));
        // 尝试获取令牌
        return limiter.tryAcquire();
    }
}
