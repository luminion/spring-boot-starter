package io.github.luminion.starter.ratelimit.support;

import io.github.luminion.starter.ratelimit.RateLimitHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * 基于 JDK 的本地限流器 (令牌桶算法)
 * 适用于无 Redis/Caffeine 的简单兜底场景
 *
 * @author luminion
 */
@Slf4j
public class JdkRateLimitHandler implements RateLimitHandler {

    public JdkRateLimitHandler() {
        log.warn("[Luminion Starter] JdkRateLimitHandler is used as a fallback implementation. " +
                "This handler is not suitable for distributed environments and may cause rate limiting to be inaccurate. " +
                "Consider using Redis, Redisson, or Caffeine for distributed rate limiting.");
    }

    /**
     * TokenBucket 状态: [0] -> tokens, [1] -> lastRefillTimestamp(nanos)
     */
    private final ConcurrentHashMap<String, long[]> bucketMap = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, double rate, long timeout, TimeUnit unit) {
        long capacity = (long) Math.max(1, rate);
        long intervalNanos = unit.toNanos(timeout);
        // fillRate = rate / (timeout秒数)，即每秒补充的令牌数
        double fillRate = rate * 1_000_000_000L / intervalNanos;

        long[] bucket = bucketMap.compute(key, (k, state) -> {
            long now = System.nanoTime();

            if (state == null) {
                // 首次初始化：满令牌桶
                return new long[]{capacity, now};
            }

            long tokens = state[0];
            long lastRefill = state[1];
            long nanosSinceLastRefill = now - lastRefill;

            // 计算应补充的令牌数
            long newTokens = (long) (nanosSinceLastRefill * fillRate / 1_000_000_000L);
            if (newTokens > 0) {
                tokens = Math.min(capacity, tokens + newTokens);
                lastRefill = now;
            }

            return new long[]{tokens, lastRefill};
        });

        // 尝试获取令牌
        if (bucket[0] >= 1) {
            bucket[0]--;
            return true;
        }
        return false;
    }
}
