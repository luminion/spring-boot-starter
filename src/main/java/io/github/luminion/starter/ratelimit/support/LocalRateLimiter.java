package io.github.luminion.starter.ratelimit.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.luminion.starter.ratelimit.RateLimiter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于 Caffeine 的本地令牌桶限流器
 * <p>
 * 适用于单机环境
 *
 * @author luminion
 */
public class LocalRateLimiter implements RateLimiter {

    /**
     * 存储令牌桶信息
     */
    private final Cache<String, TokenBucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    @Override
    public boolean tryAcquire(String key, double rate, double burst) {
        TokenBucket bucket = buckets.get(key, k -> new TokenBucket((long) burst));
        return bucket.tryAcquire(rate, (long) burst);
    }

    private static class TokenBucket {
        private final AtomicLong tokens;
        private volatile long lastRefillTimestamp;

        public TokenBucket(long capacity) {
            this.tokens = new AtomicLong(capacity);
            this.lastRefillTimestamp = System.nanoTime();
        }

        public synchronized boolean tryAcquire(double rate, long capacity) {
            long now = System.nanoTime();
            long nanosSinceLastRefill = now - lastRefillTimestamp;

            // 计算新增令牌数
            long newTokens = (long) (nanosSinceLastRefill * rate / 1_000_000_000L);

            if (newTokens > 0) {
                long currentTokens = tokens.get();
                long updatedTokens = Math.min(capacity, currentTokens + newTokens);
                tokens.set(updatedTokens);
                lastRefillTimestamp = now;
            }

            if (tokens.get() >= 1) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }
    }
}
