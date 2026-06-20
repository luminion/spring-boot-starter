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

        // trySetRate 只在 rate limiter 不存在时才设置成功，返回 true 表示本次创建了配置。
        // 利用返回值避免额外的 isExists() 调用，减少 Redis 往返。
        boolean created = rateLimiter.trySetRate(RateType.OVERALL, rateValue, interval, keepAlive);
        if (!created) {
            // 已存在，检查配置是否匹配，不匹配则更新
            RateLimiterConfig currentConfig = rateLimiter.getConfig();
            if (!matches(currentConfig, rateValue, interval.toMillis())) {
                rateLimiter.setRate(RateType.OVERALL, rateValue, interval, keepAlive);
            }
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
