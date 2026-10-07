package io.github.luminion.velo.lock.annotation;

import java.lang.annotation.*;

/**
 * 分布式锁注解。
 * 只锁住经过 AOP 代理的当前调用，正常返回或同步抛出异常即释放；不延长至异步任务完成
 * 或外层事务提交。需要覆盖完整业务过程时，由业务安排代理调用点及事务边界。
 *
 * @author luminion
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lock {

    /**
     * 用于生成锁键后缀的 SpEL 表达式，例如 {@code "#orderId"}。
     * 为空时不拼接任何参数；常量字符串需使用 SpEL 字面量，例如 {@code "'all'"}。
     */
    String value() default "";

    /**
     * 固定资源前缀，不解析 SpEL。为空时使用实际用户类全名、方法名和参数类型生成方法指纹。
     * 显式指定后替代方法指纹，不同方法或类可通过相同 prefix 和 value 结果共享锁。
     * 最终键为配置的功能前缀 + ':' + 资源前缀或方法指纹 + 可选的 ':SpEL结果'。
     * prefix 和 value 都为空时，该方法所有调用共享一把锁。
     */
    String prefix() default "";

    /**
     * 失败提示信息
     */
    String message() default "系统繁忙，请稍后再试";

}
