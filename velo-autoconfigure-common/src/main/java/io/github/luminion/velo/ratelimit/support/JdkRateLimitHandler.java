package io.github.luminion.velo.ratelimit.support;

import io.github.luminion.velo.ratelimit.RateLimitHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 JDK 的本地限流器。
 */
@Slf4j
public class JdkRateLimitHandler implements RateLimitHandler {
    private static final long MIN_IDLE_EVICT_NANOS = TimeUnit.MINUTES.toNanos(5);

    public JdkRateLimitHandler() {
        log.warn("[Velo Starter] JdkRateLimitHandler is used as a fallback implementation. " +
                "This handler is not suitable for distributed environments and may cause rate limiting to be inaccurate. " +
                "Consider using Redis, Redisson, or Caffeine for distributed rate limiting.");
    }

    private final ConcurrentHashMap<String, TokenBucket> bucketMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isCleaning = new AtomicBoolean(false);

    @Override
    public boolean tryAcquire(String key, double rate, long timeout, TimeUnit unit) {
        RateLimitWindow window = RateLimitWindow.from(rate, timeout, unit);
        long now = System.nanoTime();
        boolean acquired = bucketMap.computeIfAbsent(key, unused -> new TokenBucket())
                .tryAcquire(window.capacity(), window.intervalNanos(), now);
        if (bucketMap.mappingCount() > 1024 && isCleaning.compareAndSet(false, true)) {
            CompletableFuture.runAsync(() -> {
                try {
                    long current = System.nanoTime();
                    bucketMap.entrySet().removeIf(entry -> entry.getValue().isExpired(current));
                } finally {
                    isCleaning.set(false);
                }
            });
        }
        return acquired;
    }

    private static final class TokenBucket {
        private double tokens;
        private long lastRefillNanos;
        private long capacity;
        private long intervalNanos;
        private long evictAtNanos;

        private synchronized boolean tryAcquire(long resolvedCapacity, long resolvedIntervalNanos, long now) {
            if (lastRefillNanos == 0L) {
                capacity = resolvedCapacity;
                intervalNanos = resolvedIntervalNanos;
                tokens = resolvedCapacity;
                lastRefillNanos = now;
            } else if (capacity != resolvedCapacity || intervalNanos != resolvedIntervalNanos) {
                capacity = resolvedCapacity;
                intervalNanos = resolvedIntervalNanos;
                tokens = Math.min(tokens, resolvedCapacity);
            }

            long nanosSinceLastRefill = now - lastRefillNanos;
            double newTokens = nanosSinceLastRefill * capacity / (double) intervalNanos;
            if (newTokens > 0D) {
                tokens = Math.min(capacity, tokens + newTokens);
                lastRefillNanos = now;
            }
            evictAtNanos = saturatingAdd(now, Math.max(MIN_IDLE_EVICT_NANOS, intervalNanos));

            if (tokens >= 1D) {
                tokens -= 1D;
                return true;
            }
            return false;
        }

        private synchronized boolean isExpired(long now) {
            return evictAtNanos > 0L && now >= evictAtNanos;
        }

        private long saturatingAdd(long left, long right) {
            long result = left + right;
            return result < 0L ? Long.MAX_VALUE : result;
        }
    }
}
