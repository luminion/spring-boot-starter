package io.github.luminion.autoconfigure.aop.spi.limiter;

import io.github.luminion.autoconfigure.aop.annotation.RateLimit;
import io.github.luminion.autoconfigure.aop.spi.RateLimiter;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于ConcurrentHashMap的滑动窗口日志限流
 * <p>此实现不依赖任何外部库, 但在高并发下可能会有更高的内存占用。
 * @author luminion
 */
public class ConcurrentHashMapRateLimiter implements RateLimiter {

    private final ConcurrentMap<String, Queue<Long>> windows = new ConcurrentHashMap<>();

    @Override
    public boolean doLimit(String signature, RateLimit rateLimit) {
        long now = System.currentTimeMillis();
        long windowSize = (long) rateLimit.seconds() * 1000;
        int count = rateLimit.count();

        // 获取或创建当前key的时间戳队列
        Queue<Long> window = windows.computeIfAbsent(signature, k -> new ConcurrentLinkedQueue<>());

        // 移除窗口外的过期时间戳
        // peek()不会移除元素, poll()会移除
        while (!window.isEmpty() && now - window.peek() > windowSize) {
            window.poll();
        }

        // 如果窗口内的请求数量小于限制, 则记录当前请求时间戳并放行
        if (window.size() < count) {
            window.offer(now);
            return true;
        }

        return false;
    }
}
