package io.github.luminion.velo.ratelimit.support;

import io.github.luminion.velo.ratelimit.RateLimitHandler;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateLimiterConfig;
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
        RateLimitWindow window = RateLimitWindow.from(rate, timeout, unit);
        long rateValue = window.capacity();
        Duration interval = Duration.ofMillis(window.intervalMillis());
        Duration keepAlive = Duration.ofMillis(Math.max(window.intervalMillis(), 1000L));

        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);

        if (!rateLimiter.isExists()) {
            rateLimiter.trySetRate(RateType.OVERALL, rateValue, interval, keepAlive);
        }

        RateLimiterConfig currentConfig = rateLimiter.getConfig();
        if (!matches(currentConfig, rateValue, interval.toMillis())) {
            rateLimiter.setRate(RateType.OVERALL, rateValue, interval, keepAlive);
        }

        rateLimiter.expire(keepAlive);
        return rateLimiter.tryAcquire();
    }

    private boolean matches(RateLimiterConfig config, long rateValue, long intervalMillis) {
        return config != null
                && config.getRateType() == RateType.OVERALL
                && Long.valueOf(rateValue).equals(config.getRate())
                && Long.valueOf(intervalMillis).equals(config.getRateInterval());
    }
}
