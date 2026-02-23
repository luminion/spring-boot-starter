package io.github.luminion.starter.ratelimit.annotation;

import java.lang.annotation.*;

/**
 * 限流注解
 *
 * @author luminion
 * @since 1.0.0
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流速率 (QPS)
     * 默认每秒 10 次
     */
    double value() default 10;

    /**
     * 用于生成限流 Key 的 SpEL 表达式
     * <p>
     * 例如: "#user.id", "#request.getHeader('token')"
     * 如果为空，将根据 {@link #limitType()} 自动生成
     */
    String key() default "";

    /**
     * 限流类型
     */
    LimitType limitType() default LimitType.DEFAULT;

    /**
     * 提示信息
     */
    String message() default "当前访问人数较多，请稍后再试";

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
