package io.github.luminion.velo.ratelimit.support;

import io.github.luminion.velo.ratelimit.RateLimitHandler;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateLimiterConfig;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

import java.time.Duration;

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
    public boolean tryAcquire(String key, int qps) {
        if (qps <= 0) {
            throw new IllegalArgumentException("Rate limit qps must be greater than zero.");
        }
        long rateValue = qps;
        Duration interval = Duration.ofSeconds(1);
        Duration keepAlive = interval;

        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);

        // 稳定配置只需读取、刷新 TTL 和申请额度，省去每次都尝试初始化的调用。
        // 显式刷新保留对已有 keepAlive=0 配置的支持；读取后已过期则重新初始化。
        RateLimiterConfig currentConfig = rateLimiter.getConfig();
        if (matches(currentConfig, rateValue, interval.toMillis()) && rateLimiter.expire(keepAlive)) {
            return rateLimiter.tryAcquire();
        }

        boolean created = rateLimiter.trySetRate(RateType.OVERALL, rateValue, interval, keepAlive);
        if (!created) {
            // 初始化可能输给并发请求，重新读取后再决定是否更新，避免重置相同配置的额度。
            currentConfig = rateLimiter.getConfig();
            if (!matches(currentConfig, rateValue, interval.toMillis())) {
                rateLimiter.setRate(RateType.OVERALL, rateValue, interval, keepAlive);
            }
        }

        // 回退路径保留原有整体过期操作，兼容已有配置及原生派生 key。
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
