package io.github.luminion.velo.ratelimit.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.luminion.velo.ratelimit.RateLimitHandler;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Caffeine 的本地令牌桶限流器。
 */
public class CaffeineRateLimitHandler implements RateLimitHandler {

    private final Cache<String, TokenBucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    @Override
    public boolean tryAcquire(String key, double rate, long timeout, TimeUnit unit) {
        RateLimitWindow window = RateLimitWindow.from(rate, timeout, unit);
        return buckets.get(key, unused -> new TokenBucket())
                .tryAcquire(window.capacity(), window.intervalNanos());
    }

    private static final class TokenBucket {
        private double tokens;
        private long lastRefillNanos;
        private long capacity;
        private long intervalNanos;

        private synchronized boolean tryAcquire(long resolvedCapacity, long resolvedIntervalNanos) {
            if (lastRefillNanos == 0L) {
                capacity = resolvedCapacity;
                intervalNanos = resolvedIntervalNanos;
                tokens = resolvedCapacity;
                lastRefillNanos = System.nanoTime();
            } else if (capacity != resolvedCapacity || intervalNanos != resolvedIntervalNanos) {
                capacity = resolvedCapacity;
                intervalNanos = resolvedIntervalNanos;
                tokens = Math.min(tokens, resolvedCapacity);
            }

            long now = System.nanoTime();
            long nanosSinceLastRefill = now - lastRefillNanos;
            double newTokens = nanosSinceLastRefill * capacity / (double) intervalNanos;
            if (newTokens > 0D) {
                tokens = Math.min(capacity, tokens + newTokens);
                lastRefillNanos = now;
            }

            if (tokens >= 1D) {
                tokens -= 1D;
                return true;
            }
            return false;
        }
    }
}
