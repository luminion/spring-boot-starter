package io.github.luminion.velo.ratelimit.support;

import io.github.luminion.velo.ratelimit.RateLimitHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于 JDK 的本地限流器。
 */
@Slf4j
public class JdkRateLimitHandler implements RateLimitHandler {

    public JdkRateLimitHandler() {
        log.warn("[Velo Starter] JdkRateLimitHandler is used as a fallback implementation. " +
                "This handler is not suitable for distributed environments and may cause rate limiting to be inaccurate. " +
                "Consider using Redis, Redisson, or Caffeine for distributed rate limiting.");
    }

    private final ConcurrentHashMap<String, TokenBucket> bucketMap = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, double rate, long timeout, TimeUnit unit) {
        return bucketMap.computeIfAbsent(key, unused -> new TokenBucket()).tryAcquire(rate, timeout, unit);
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