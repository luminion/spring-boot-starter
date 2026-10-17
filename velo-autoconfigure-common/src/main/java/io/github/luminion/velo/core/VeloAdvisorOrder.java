package io.github.luminion.velo.core;

import org.springframework.core.Ordered;

/**
 * Starter 管理的切面执行顺序常量。
 */
public final class VeloAdvisorOrder {

    private static final int CONCURRENCY_BASE = Ordered.LOWEST_PRECEDENCE - 50_000;

    public static final int CONCURRENCY_IDEMPOTENT = CONCURRENCY_BASE;

    public static final int CONCURRENCY_RATE_LIMIT = CONCURRENCY_BASE + 10_000;

    public static final int CONCURRENCY_LOCK = CONCURRENCY_BASE + 20_000;

    /**
     * 调用日志切面的执行顺序。位于并发控制切面内侧，使被幂等、限流或锁拒绝的请求不会作为方法调用记录。
     */
    public static final int LOG_INVOKE = CONCURRENCY_BASE + 25_000;

    /**
     * Controller 日志切面的执行顺序。
     */
    public static final int LOG_CONTROLLER = CONCURRENCY_BASE + 35_000;

    /**
     * Feign 日志切面的执行顺序。
     */
    public static final int LOG_FEIGN = CONCURRENCY_BASE + 40_000;

    private VeloAdvisorOrder() {
    }
}
