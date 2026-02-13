package io.github.luminion.starter.core.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 接口幂等性注解
 * <p>
 * 用于防止重复提交或处理幂等请求
 *
 * @author luminion
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 用于生成幂等 Key 的 SpEL 表达式
     * <p>
     * 例如: "#user.id", "#request.getHeader('token')"
     * 如果为空，则根据方法指纹（类名+方法名+参数内容）自动生成
     */
    String value() default "";

    /**
     * 过期时间，默认 5 秒
     */
    long timeout() default 5;

    /**
     * 时间单位，默认秒
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * 是否在方法执行完成后自动释放 Key
     * <p>
     * true: 仅防止“并发重复提交”。方法执行完立即释放，允许下一次请求进入。
     * false: 防止“固定采样频率内的重复提交”。直到过期时间到达前，都不允许相同请求再次进入。
     */
    boolean autoRelease() default true;

    /**
     * 提示信息
     */
    String message() default "请求正在处理中，请稍后再试";

}
