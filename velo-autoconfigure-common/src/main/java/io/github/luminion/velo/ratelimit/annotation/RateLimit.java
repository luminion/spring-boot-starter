package io.github.luminion.velo.ratelimit.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

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
     * 时间窗口内允许的最大请求数。
     */
    double permits() default 50;

    /**
     * 限流时间窗口大小。
     */
    long ttl() default 1;

    /**
     * 限流时间窗口单位
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * 用于生成限流 Key 的 SpEL 表达式
     * <p>
     * 例如: "#user.id", "#request.getHeader('token')"
     * 如果为空，将退回到方法级别的固定 Key
     */
    String key() default "";

    /**
     * 提示信息
     */
    String message() default "当前访问人数较多，请稍后再试";
}
