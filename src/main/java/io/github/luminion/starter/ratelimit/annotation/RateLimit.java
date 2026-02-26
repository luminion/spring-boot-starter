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
     */
    double value() default 50;

    /**
     * 用于生成限流 Key 的 SpEL 表达式
     * <p>
     * 例如: "#user.id", "#request.getHeader('token')"
     * 如果为空，将根据方法指纹（类名+方法名+参数内容）自动生成
     */
    String key() default "";

    /**
     * 提示信息
     */
    String message() default "当前访问人数较多，请稍后再试";
}
