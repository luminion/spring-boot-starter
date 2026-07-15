package io.github.luminion.velo.idempotent.annotation;

import java.lang.annotation.*;

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
     * 用于生成幂等 Key 的 SpEL 表达式。
     * <p>
     * 为空时使用类名和方法名生成固定 Key。
     */
    String key() default "";

    /**
     * 幂等窗口 TTL，单位为毫秒。
     */
    long ttl() default 3000;

    /**
     * 提示信息
     */
    String message() default "您的请求已提交，请勿重复操作";

}
