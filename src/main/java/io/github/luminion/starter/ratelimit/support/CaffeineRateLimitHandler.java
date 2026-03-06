package io.github.luminion.starter.ratelimit.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.luminion.starter.ratelimit.RateLimitHandler;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Caffeine 的本地令牌桶限流器
 *
 * @author luminion
 */
public class CaffeineRateLimitHandler implements RateLimitHandler {

    private final Cache<String, TokenBucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    @Override
    public boolean tryAcquire(String key, double rate, long timeout, TimeUnit unit) {
        TokenBucket bucket = buckets.get(key, k -> new TokenBucket((long) rate, timeout, unit));
        return bucket.tryAcquire();
    }

    private static class TokenBucket {
        /**
         * 当前令牌数
         */
        private double tokens;
        /**
         * 上次补充令牌的时间戳（纳秒）
         */
        private long lastRefillNanos;
        /**
         * 令牌桶容量
         */
        private final long capacity;
        /**
         * 每秒补充的令牌数 = capacity / timeout秒数
         */
        private final double fillRate;

        public TokenBucket(long capacity, long timeout, TimeUnit unit) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefillNanos = System.nanoTime();
            // fillRate = capacity / (timeout秒数)，即每秒补充的令牌数
            double timeoutSeconds = timeout * unit.toNanos(1) / 1_000_000_000L;
            this.fillRate = capacity / timeoutSeconds;
        }

        /**
         * 尝试获取令牌（非线程安全，仅 Caffeine 的 expireAfterAccess 清理时可能有竞态）
         */
        public boolean tryAcquire() {
            long now = System.nanoTime();
            long nanosSinceLastRefill = now - lastRefillNanos;

            // 计算应补充的令牌数
            double newTokens = nanosSinceLastRefill * fillRate / 1_000_000_000L;
            if (newTokens > 0) {
                tokens = Math.min(capacity, tokens + newTokens);
                lastRefillNanos = now;
            }

            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }
    }
}
