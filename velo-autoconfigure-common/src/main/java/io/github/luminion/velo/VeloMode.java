package io.github.luminion.velo;

/**
 * Starter default behavior profile.
 *
 * <p>Controls which opinionated defaults are applied at startup.</p>
 *
 * @see io.github.luminion.velo.core.VeloModeEnvironmentPostProcessor
 */
public enum VeloMode {

    /**
     * Enables opinionated defaults for out-of-the-box behavior.
     * <p>
     * All starter features are enabled by default, including:
     * trace id propagation, invocation logging (controller/feign),
     * Jackson customization, date-time converters, MyBatis-Plus interceptors,
     * cache, Redis templates, Excel converters and more.
     */
    OPINIONATED,

    /**
     * Disables global behavior changes by default while keeping explicit annotations usable.
     * <p>
     * The following features are disabled in CONSERVATIVE mode:
     * <ul>
     *   <li>Trace id creation and propagation ({@code velo.log.trace.enabled=false})</li>
     *   <li>Controller and Feign invocation logging ({@code velo.log.invocation.controller.enabled=false}, {@code velo.log.invocation.feign.enabled=false})</li>
     *   <li>Jackson customization ({@code velo.jackson.enabled=false})</li>
     *   <li>Date-time Spring converters ({@code velo.spring-converter.date-time-enabled=false})</li>
     *   <li>MyBatis-Plus interceptors ({@code velo.mybatis-plus.enabled=false})</li>
     *   <li>Cache auto-configuration ({@code velo.cache.enabled=false})</li>
     *   <li>Redis templates ({@code velo.redis.enabled=false})</li>
     *   <li>Excel converters ({@code velo.excel.converters.enabled=false})</li>
     * </ul>
     * Features that remain active: {@code @Lock}, {@code @Idempotent}, {@code @RateLimit},
     * XSS protection, method-level {@code @InvokeLog}/{@code @SlowLog}.
     */
    CONSERVATIVE
}
