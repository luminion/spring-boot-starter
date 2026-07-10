package io.github.luminion.velo.ratelimit.support;

import io.github.luminion.velo.ratelimit.RateLimitHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 JDK 的本地限流器。
 */
@Slf4j
public class JdkRateLimitHandler implements RateLimitHandler, DisposableBean {
    private static final long MIN_IDLE_EVICT_NANOS = TimeUnit.MINUTES.toNanos(5);

    public JdkRateLimitHandler() {
        log.warn("[Velo Starter] JdkRateLimitHandler is used as a fallback implementation. " +
                "This handler is not suitable for distributed environments and may cause rate limiting to be inaccurate. " +
                "Consider using Redis, Redisson, or Caffeine for distributed rate limiting.");
    }

    private final ConcurrentHashMap<String, TokenBucket> bucketMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isCleaning = new AtomicBoolean(false);
    private final ExecutorService cleanupExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "velo-ratelimit-cleanup");
        t.setDaemon(true);
        return t;
    });

    @Override
    public boolean tryAcquire(String key, double rate, long timeout, TimeUnit unit) {
        RateLimitWindow window = RateLimitWindow.from(rate, timeout, unit);
        long now = System.nanoTime();
        AtomicBoolean acquired = new AtomicBoolean(false);
        bucketMap.compute(key, (unused, existing) -> {
            TokenBucket bucket = existing != null ? existing : new TokenBucket();
            acquired.set(bucket.tryAcquire(window.capacity(), window.intervalNanos(), now));
            return bucket;
        });
        if (bucketMap.mappingCount() > 1024 && isCleaning.compareAndSet(false, true)) {
            try {
                cleanupExecutor.execute(() -> {
                    try {
                        long current = System.nanoTime();
                        for (String bucketKey : bucketMap.keySet()) {
                            bucketMap.computeIfPresent(bucketKey,
                                    (unused, bucket) -> bucket.isExpired(current) ? null : bucket);
                        }
                    } finally {
                        isCleaning.set(false);
                    }
                });
            } catch (RejectedExecutionException ignored) {
                // 容器关闭后线程池已 shutdown，清理提交被拒属正常；复位标志避免永久 true 再不触发清理
                isCleaning.set(false);
            }
        }
        return acquired.get();
    }

    @Override
    public void destroy() {
        cleanupExecutor.shutdownNow();
    }

    private static final class TokenBucket {
        private double tokens;
        private long lastRefillNanos;
        private long capacity;
        private long intervalNanos;
        private long lastAccessNanos;
        private long idleEvictNanos;

        private boolean tryAcquire(long resolvedCapacity, long resolvedIntervalNanos, long now) {
            if (lastRefillNanos == 0L) {
                capacity = resolvedCapacity;
                intervalNanos = resolvedIntervalNanos;
                tokens = resolvedCapacity;
                lastRefillNanos = now;
            } else if (capacity != resolvedCapacity || intervalNanos != resolvedIntervalNanos) {
                capacity = resolvedCapacity;
                intervalNanos = resolvedIntervalNanos;
                tokens = Math.min(tokens, resolvedCapacity);
            }

            long nanosSinceLastRefill = now - lastRefillNanos;
            // 先转 double 再乘，避免 long * long 中间结果在高速率+长空闲时静默溢出为负导致令牌永不补充
            double newTokens = (double) nanosSinceLastRefill * capacity / (double) intervalNanos;
            if (newTokens > 0D) {
                tokens = Math.min(capacity, tokens + newTokens);
                lastRefillNanos = now;
            }
            lastAccessNanos = now;
            idleEvictNanos = Math.max(MIN_IDLE_EVICT_NANOS, intervalNanos);

            if (tokens >= 1D) {
                tokens -= 1D;
                return true;
            }
            return false;
        }

        private boolean isExpired(long now) {
            return lastAccessNanos != 0L && now - lastAccessNanos >= idleEvictNanos;
        }
    }
}
