package io.github.luminion.velo.ratelimit.support;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import io.github.luminion.velo.ratelimit.RateLimitHandler;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 使用 Guava 原生限流器，按 key 共享额度，仅在当前 JVM 内生效。
 * <p>限流器空闲五分钟后由 Guava 原生缓存过期；不创建清理线程或定时任务。
 * 不设置容量淘汰或弱引用，避免活跃限流器被提前移除后重新获得额度。
 * 额度补充和空闲时的突发额度遵循 Guava 原生行为。
 */
public class GuavaRateLimitHandler implements RateLimitHandler {

    private final Cache<String, RateLimiter> limiters;

    public GuavaRateLimitHandler() {
        this(Ticker.systemTicker());
    }

    GuavaRateLimitHandler(Ticker ticker) {
        this.limiters = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .ticker(ticker)
                .build();
    }

    @Override
    public boolean tryAcquire(String key, int qps) {
        if (qps <= 0) {
            throw new IllegalArgumentException("qps must be positive");
        }
        try {
            RateLimiter limiter = limiters.get(key, () -> RateLimiter.create(qps));
            if (limiter.getRate() != qps) {
                limiter.setRate(qps);
            }
            return limiter.tryAcquire();
        } catch (ExecutionException ex) {
            throw new IllegalStateException("Cannot create Guava RateLimiter for key: " + key, ex.getCause());
        }
    }
}
