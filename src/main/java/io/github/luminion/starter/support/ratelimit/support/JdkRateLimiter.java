package io.github.luminion.starter.support.ratelimit.support;

import io.github.luminion.starter.support.ratelimit.annotation.RateLimit;
import io.github.luminion.starter.support.ratelimit.spi.RateLimiter;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于JDK {@link ConcurrentHashMap} 的滑动窗口限流处理器。
 * <p>这是一个不依赖任何外部库的兜底实现。
 * <p><b>警告：此实现存在内存泄漏风险！</b>
 * <p>当使用动态key（例如，基于用户ID或IP地址的SpEL表达式）时，
 * 用于存储时间窗口的Map会无限增长，并可能在长时间运行后导致 {@code OutOfMemoryError}。
 * <p><b>因此，它只应在以下两种情况被使用：</b>
 * <ol>
 *     <li>作为其他更优先的Handler（如Redis, Caffeine, Guava）都未引入时的最终兜底方案。</li>
 *     <li>当可以确保限流key的数量是固定的、有限的时候。</li>
 * </ol>
 *
 * @author luminion
 */
public class JdkRateLimiter implements RateLimiter {

    private final ConcurrentMap<String, Queue<Long>> windows = new ConcurrentHashMap<>();

    @Override
    public boolean tryAccess(String signature, RateLimit rateLimit) {
        long now = System.currentTimeMillis();
        long windowSize = (long) rateLimit.seconds() * 1000;
        int count = rateLimit.count();

        // 获取或创建当前key的时间戳队列
        Queue<Long> window = windows.computeIfAbsent(signature, k -> new ConcurrentLinkedQueue<>());

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
