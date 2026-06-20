package io.github.luminion.velo.core;

import org.springframework.core.Ordered;

/**
 * Starter managed advisor order values.
 */
public final class VeloAdvisorOrder {

    private static final int CONCURRENCY_BASE = Ordered.LOWEST_PRECEDENCE - 30_000;

    public static final int CONCURRENCY_IDEMPOTENT = CONCURRENCY_BASE;

    public static final int CONCURRENCY_RATE_LIMIT = CONCURRENCY_BASE + 10_000;

    public static final int CONCURRENCY_LOCK = CONCURRENCY_BASE + 20_000;

    /**
     * Invocation log aspect order. Positioned inside concurrency control aspects so that
     * rejected requests (idempotent/rate-limit/lock) are not logged as method invocations.
     */
    public static final int LOG_INVOKE = CONCURRENCY_BASE + 25_000;

    /**
     * Slow log aspect order. Positioned inside {@link #LOG_INVOKE} so that when both
     * {@code @InvokeLog} and {@code @SlowLog} are present, {@code InvokeLogAspect} wraps
     * {@code SlowLogAspect} and the latter skips via {@code hasInvokeLog} guard.
     */
    public static final int LOG_SLOW = CONCURRENCY_BASE + 30_000;

    /**
     * Controller log aspect order.
     */
    public static final int LOG_CONTROLLER = CONCURRENCY_BASE + 35_000;

    /**
     * Feign log aspect order.
     */
    public static final int LOG_FEIGN = CONCURRENCY_BASE + 40_000;

    private VeloAdvisorOrder() {
    }
}
