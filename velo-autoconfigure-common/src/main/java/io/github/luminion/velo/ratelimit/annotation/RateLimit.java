package io.github.luminion.velo.ratelimit.annotation;

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
     * 用于生成限流桶后缀的 SpEL 表达式，例如 {@code "#userId"}。
     * 为空时不拼接任何参数；常量字符串需使用 SpEL 字面量，例如 {@code "'all'"}。
     */
    String value() default "";

    /**
     * 固定资源前缀，不解析 SpEL。为空时使用实际用户类全名、方法名和参数类型生成方法指纹。
     * 显式指定后替代方法指纹，不同方法或类可通过相同 prefix 和 value 结果共享限流额度。
     * 最终键为配置的功能前缀 + ':' + 资源前缀或方法指纹 + 可选的 ':SpEL结果'。
     * value 为空时所有调用者共享该范围的额度；共享范围必须使用相同 qps。
     */
    String prefix() default "";

    /**
     * 每秒请求速率，必须为正整数；额度补充节奏由具体后端决定。
     * Redis 为固定一秒窗口，允许窗口边界突发，不限制任意滚动一秒内的总量。
     */
    int qps() default 50;

    /**
     * 限流被拒绝时的提示信息，支持普通文本或 {@code {i18n.key}}。
     * 为空或仅含空白时，使用全局配置 {@code velo.rate-limit.message}。
     */
    String message() default "";
}
