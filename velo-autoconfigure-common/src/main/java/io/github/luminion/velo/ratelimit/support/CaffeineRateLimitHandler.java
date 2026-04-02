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
        return buckets.get(key, unused -> new TokenBucket()).tryAcquire(rate, timeout, unit);
    }

    private static final class TokenBucket {
        private double tokens;
        private long lastRefillNanos;
        private long capacity;
        private double fillRate;

        private synchronized boolean tryAcquire(double rate, long timeout, TimeUnit unit) {
            long resolvedCapacity = Math.max(1L, (long) Math.ceil(rate));
            long intervalNanos = Math.max(1L, unit.toNanos(timeout));
            double resolvedFillRate = rate * 1_000_000_000D / intervalNanos;

            if (lastRefillNanos == 0L) {
                capacity = resolvedCapacity;
                fillRate = resolvedFillRate;
                tokens = resolvedCapacity;
                lastRefillNanos = System.nanoTime();
            } else if (capacity != resolvedCapacity || Double.compare(fillRate, resolvedFillRate) != 0) {
                capacity = resolvedCapacity;
                fillRate = resolvedFillRate;
                tokens = Math.min(tokens, resolvedCapacity);
            }

            long now = System.nanoTime();
            long nanosSinceLastRefill = now - lastRefillNanos;
            double newTokens = nanosSinceLastRefill * fillRate / 1_000_000_000D;
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