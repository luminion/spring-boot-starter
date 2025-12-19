package io.github.luminion.autoconfigure.aop.support.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.luminion.autoconfigure.aop.annotation.RateLimit;
import io.github.luminion.autoconfigure.aop.core.RateLimiter;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * 基于Caffeine的、内存安全的滑动窗口限流处理器。
 * <p>这是推荐的本地内存限流实现。
 * <p><b>依赖:</b> {@code com.github.benmanes.caffeine:caffeine}
 * <p>
 * 注意版本: For Java 11 or above, use 3.x otherwise use 2.x.
 * @author luminion
 */
public class CaffeineRateLimitHandler implements RateLimiter {

    // 使用一个统一的Caffeine Cache实例来存储所有的时间戳队列。
    // 当一个限流器在10分钟内没有被访问时，它将被自动回收，从而防止内存泄漏。
    private final Cache<String, Queue<Long>> windows = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    @Override
    public boolean tryAccess(String signature, RateLimit rateLimit) {
        long now = System.currentTimeMillis();
        long windowSize = (long) rateLimit.seconds() * 1000;
        int count = rateLimit.count();

        // 从缓存中获取或创建当前key的时间戳队列
        Queue<Long> window = windows.get(signature, k -> new ConcurrentLinkedQueue<>());

        // 移除窗口外的过期时间戳 (此操作线程安全，无需同步)
        while (!window.isEmpty() && now - window.peek() > windowSize) {
            window.poll();
        }

        // 在检查和修改操作之间进行同步，保证原子性
        synchronized (window) {
            if (window.size() < count) {
                window.offer(now);
                return true;
            }
        }

        return false;
    }
}
