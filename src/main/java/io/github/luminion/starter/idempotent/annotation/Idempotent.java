package io.github.luminion.starter.idempotent.annotation;

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
     * 过期时间
     */
    long timeout() default 3;

    /**
     * 时间单位，默认秒
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * 提示信息
     */
    String message() default "您的请求已提交，请勿重复操作";

}
