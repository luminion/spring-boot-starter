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
        RateLimitWindow window = RateLimitWindow.from(rate, timeout, unit);
        return bucketMap.computeIfAbsent(key, unused -> new TokenBucket())
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
