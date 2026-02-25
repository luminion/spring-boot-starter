package io.github.luminion.starter.feature.ratelimit.support;

import io.github.luminion.starter.feature.ratelimit.RateLimitHandler;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 JDK 的本地限流器 (固定窗口算法)
 * 适用于无 Redis/Caffeine/Guava 的简单兜底场景
 *
 * @author luminion
 */
public class JdkRateLimitHandler implements RateLimitHandler {

    /**
     * Key: 限流键
     * Value: [0] -> 当前窗口计数, [1] -> 窗口起始时间戳 (ms)
     */
    private final ConcurrentHashMap<String, long[]> windowMap = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, double rate) {
        long limit = (long) rate;
        if (limit <= 0)
            return false;

        long now = System.currentTimeMillis();
        long windowSize = 1000; // 固定 1 秒窗口

        long[] state = windowMap.compute(key, (k, v) -> {
            if (v == null || (now - v[1]) > windowSize) {
                // 新窗口
                return new long[] { 1, now };
            }
            // 旧窗口累加
            v[0]++;
            return v;
        });

        // 定期清理过期窗口
        if (windowMap.size() > 1024) {
            windowMap.entrySet().removeIf(entry -> (now - entry.getValue()[1]) > windowSize * 10);
        }

        return state[0] <= limit;
    }
}
