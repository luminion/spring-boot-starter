package io.github.luminion.velo.ratelimit.support;

import java.util.concurrent.TimeUnit;

/**
 * Shared normalized rate-limit window settings.
 */
final class RateLimitWindow {

    private final long capacity;
    private final long intervalMillis;
    private final long intervalNanos;

    private RateLimitWindow(long capacity, long intervalMillis, long intervalNanos) {
        this.capacity = capacity;
        this.intervalMillis = intervalMillis;
        this.intervalNanos = intervalNanos;
    }

    static RateLimitWindow from(double permits, long window) {
        if (!Double.isFinite(permits) || permits <= 0D) {
            throw new IllegalArgumentException("Rate limit permits must be a finite positive number.");
        }
        if (window <= 0L) {
            throw new IllegalArgumentException("Rate limit window must be greater than zero.");
        }

        long capacity = Math.max(1L, (long) Math.ceil(permits));
        double scale = capacity / permits;
        long baseMillis = window;
        long baseNanos = TimeUnit.MILLISECONDS.toNanos(window);

        return new RateLimitWindow(
                capacity,
                ceilToLong(baseMillis * scale),
                ceilToLong(baseNanos * scale));
    }

    long capacity() {
        return capacity;
    }

    long intervalMillis() {
        return intervalMillis;
    }

    long intervalNanos() {
        return intervalNanos;
    }

    private static long ceilToLong(double value) {
        if (value >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, (long) Math.ceil(value));
    }
}
