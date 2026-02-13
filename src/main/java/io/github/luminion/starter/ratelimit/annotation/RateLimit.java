package io.github.luminion.starter.ratelimit.annotation;

import java.lang.annotation.*;

/**
 * 限流注解
 *
 * @author luminion
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 用于生成限流 Key 的 SpEL 表达式
     * <p>
     * 如果为空，将根据 {@link #limitType()} 自动生成
     */
    String value() default "";

    /**
     * 每秒速率 (QPS)
     */
    double rate() default 10;

    /**
     * 突发流量大小 (令牌桶容量)
     * 默认与 rate 相同
     */
    double burst() default 0;

    /**
     * 限流类型
     */
    LimitType limitType() default LimitType.DEFAULT;

    /**
     * 提示信息
     */
    String message() default "访问过于频繁，请稍后再试";

    /**
     * 限流类型枚举
     */
    enum LimitType {
        /**
         * 默认：类名#方法名
         */
        DEFAULT,
        /**
         * IP 维度
         */
        IP,
        /**
         * 用户维度 (需要当前请求上下文)
         */
        USER,
        /**
         * 全局维度 (整个接口共用一个桶)
         */
        GLOBAL
    }
}
