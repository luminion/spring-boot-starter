package io.github.luminion.velo.core;

import org.springframework.core.Ordered;

/**
 * Starter managed advisor order values.
 */
public final class VeloAdvisorOrder {

    private static final int CONCURRENCY_BASE = Ordered.LOWEST_PRECEDENCE - 50_000;

    public static final int CONCURRENCY_IDEMPOTENT = CONCURRENCY_BASE;

    public static final int CONCURRENCY_RATE_LIMIT = CONCURRENCY_BASE + 10_000;

    public static final int CONCURRENCY_LOCK = CONCURRENCY_BASE + 20_000;

    /**
     * Invocation log aspect order. Positioned inside concurrency control aspects so that
     * rejected requests (idempotent/rate-limit/lock) are not logged as method invocations.
     */
    public static final int LOG_INVOKE = CONCURRENCY_BASE + 25_000;

    /**
     * Slow log aspect order. It wraps the invocation log aspects so that its record is
     * written last while the advisor chain unwinds after the business invocation.
     */
    public static final int LOG_SLOW = CONCURRENCY_BASE + 24_000;

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
