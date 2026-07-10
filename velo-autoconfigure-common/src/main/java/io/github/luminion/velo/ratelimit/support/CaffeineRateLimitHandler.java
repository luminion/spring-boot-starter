package io.github.luminion.velo.ratelimit.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Ticker;
import io.github.luminion.velo.ratelimit.RateLimitHandler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 Caffeine 的本地令牌桶限流器。
 */
public class CaffeineRateLimitHandler implements RateLimitHandler {

    private static final long MIN_IDLE_EVICT_NANOS = TimeUnit.MINUTES.toNanos(5);

    private final Ticker ticker;
    private final Cache<String, TokenBucket> buckets;

    public CaffeineRateLimitHandler() {
        this(Ticker.systemTicker());
    }

    CaffeineRateLimitHandler(Ticker ticker) {
        this.ticker = ticker;
        this.buckets = Caffeine.newBuilder()
                .ticker(ticker)
                .expireAfter(new Expiry<String, TokenBucket>() {
                    @Override
                    public long expireAfterCreate(String key, TokenBucket bucket, long currentTime) {
                        return bucket.idleEvictNanos();
                    }

                    @Override
                    public long expireAfterUpdate(String key, TokenBucket bucket, long currentTime,
                            long currentDuration) {
                        return bucket.idleEvictNanos();
                    }

                    @Override
                    public long expireAfterRead(String key, TokenBucket bucket, long currentTime,
                            long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    @Override
    public boolean tryAcquire(String key, double rate, long timeout, TimeUnit unit) {
        RateLimitWindow window = RateLimitWindow.from(rate, timeout, unit);
        long now = ticker.read();
        AtomicBoolean acquired = new AtomicBoolean(false);
        buckets.asMap().compute(key, (unused, existing) -> {
            TokenBucket bucket = existing != null ? existing : new TokenBucket();
            acquired.set(bucket.tryAcquire(window.capacity(), window.intervalNanos(), now));
            return bucket;
        });
        return acquired.get();
    }

    private static final class TokenBucket {
        private double tokens;
        private long lastRefillNanos;
        private long capacity;
        private long intervalNanos;
        private long idleEvictNanos = MIN_IDLE_EVICT_NANOS;

        private boolean tryAcquire(long resolvedCapacity, long resolvedIntervalNanos, long now) {
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
            // 先转 double 再乘，避免 long * long 中间结果在高速率+长空闲时静默溢出为负导致令牌永不补充
            double newTokens = (double) nanosSinceLastRefill * capacity / (double) intervalNanos;
            if (newTokens > 0D) {
                tokens = Math.min(capacity, tokens + newTokens);
                lastRefillNanos = now;
            }
            idleEvictNanos = Math.max(MIN_IDLE_EVICT_NANOS, intervalNanos);

            if (tokens >= 1D) {
                tokens -= 1D;
                return true;
            }
            return false;
        }

        private long idleEvictNanos() {
            return idleEvictNanos;
        }
    }
}
