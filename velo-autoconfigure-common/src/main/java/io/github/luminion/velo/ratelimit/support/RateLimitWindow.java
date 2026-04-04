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

    static RateLimitWindow from(double permits, long timeout, TimeUnit unit) {
        if (!Double.isFinite(permits) || permits <= 0D) {
            throw new IllegalArgumentException("Rate limit permits must be a finite positive number.");
        }
        if (timeout <= 0L) {
            throw new IllegalArgumentException("Rate limit timeout must be greater than zero.");
        }

        long capacity = Math.max(1L, (long) Math.ceil(permits));
        double scale = capacity / permits;
        long baseMillis = Math.max(1L, unit.toMillis(timeout));
        long baseNanos = Math.max(1L, unit.toNanos(timeout));

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
