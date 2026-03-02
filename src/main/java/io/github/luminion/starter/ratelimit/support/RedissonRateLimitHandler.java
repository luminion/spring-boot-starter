package io.github.luminion.starter.ratelimit.support;

import io.github.luminion.starter.ratelimit.RateLimitHandler;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

/**
 * 基于 Redisson 的分布式限流器
 *
 * @author luminion
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class RedissonRateLimitHandler implements RateLimitHandler {

    private final RedissonClient redissonClient;

    @Override
    public boolean tryAcquire(String key, double rate) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        // 设置速率，如果不存在则初始化
        // Redisson 的速率是每 intervalUnit 产生 rate 个令牌
        long rateValue = (long) Math.max(1, rate);
        rateLimiter.trySetRate(RateType.OVERALL, rateValue, 1, RateIntervalUnit.SECONDS);
        return rateLimiter.tryAcquire();
    }
}
