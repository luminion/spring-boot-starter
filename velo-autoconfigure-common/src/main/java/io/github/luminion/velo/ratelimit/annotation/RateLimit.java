package io.github.luminion.velo.ratelimit.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 限流注解
 *
 * @author luminion
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * QPS 的简写属性。
     */
    @AliasFor("qps")
    int value() default 50;

    /**
     * 每秒请求速率，必须为正整数；额度补充节奏由具体后端决定。
     * Redis 为固定一秒窗口，允许窗口边界突发，不限制任意滚动一秒内的总量。
     */
    @AliasFor("value")
    int qps() default 50;

    /**
     * 用于生成限流分桶后缀的 SpEL 表达式。
     * <p>
     * 例如: "#user.id", "#request.getHeader('token')"
     * 如果为空，将直接使用方法级别的固定 Key；
     * 如果不为空，最终 Key 为“方法级固定 Key + ':' + 表达式结果”。
     */
    String key() default "";

    /**
     * 提示信息
     */
    String message() default "当前访问人数较多，请稍后再试";
}
