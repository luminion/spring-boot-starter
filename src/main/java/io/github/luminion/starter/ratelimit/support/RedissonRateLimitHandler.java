package io.github.luminion.starter.ratelimit.support;

import io.github.luminion.starter.ratelimit.RateLimitHandler;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

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
    public boolean tryAcquire(String key, double rate, long timeout, TimeUnit unit) {
        long rateValue = (long) Math.max(1, rate);
        long intervalMillis = unit.toMillis(timeout);

        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        
        // trySetRate 对已存在的限流器不生效，需要先删除再设置
        // 但频繁删除会导致限流器状态丢失，这里用 isExists 做简单判断
        if (!rateLimiter.isExists()) {
            rateLimiter.trySetRate(
                    RateType.OVERALL,
                    rateValue,
                    Duration.ofMillis(intervalMillis)
            );
        }

        return rateLimiter.tryAcquire();
    }
}
