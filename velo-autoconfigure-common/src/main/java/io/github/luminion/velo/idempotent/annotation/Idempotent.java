package io.github.luminion.velo.idempotent.annotation;

import java.lang.annotation.*;

/**
 * 接口幂等性注解
 * <p>
 * 用于在 TTL 窗口内防止重复提交，不保证业务永久幂等，也不管理异步任务的完成与失败。
 *
 * @author luminion
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 用于生成防重复键后缀的 SpEL 表达式，例如 {@code "#userId"}。
     * 为空时不拼接任何参数；常量字符串需使用 SpEL 字面量，例如 {@code "'all'"}。
     */
    String value() default "";

    /**
     * 固定资源前缀，不解析 SpEL。为空时使用实际用户类全名、方法名和参数类型生成方法指纹。
     * 显式指定后替代方法指纹，不同方法或类可通过相同 prefix 和 value 结果共享防重复窗口。
     * 最终键为配置的功能前缀 + ':' + 资源前缀或方法指纹 + 可选的 ':SpEL结果'。
     * value 为空时所有调用者共享该范围的窗口；共享范围应统一 TTL 与业务含义。
     */
    String prefix() default "";

    /**
     * 幂等窗口 TTL，单位为毫秒。
     */
    long ttl() default 3000;

    /**
     * 防重复提交被拒绝时的提示信息，支持普通文本或 {@code {i18n.key}}。
     * 为空或仅含空白时，使用全局配置 {@code velo.idempotent.message}。
     */
    String message() default "";

}
