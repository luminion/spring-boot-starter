package io.github.luminion.starter.repeat.annotation;

import io.github.luminion.starter.core.aop.KeyResolver;
import io.github.luminion.starter.repeat.spi.RepeatSubmitHandler;

import java.lang.annotation.*;

/**
 * 防重复提交注解
 * <p>
 * 用于标记需要防止重复提交的方法，可以通过配置表达式、处理器等来控制防重复提交的行为
 *
 * @author luminion
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {

    /**
     * 用于计算签名的表达式。指定的{@link #keyResolver()}将根据此处指定的表达式计算一个唯一签名。
     * <p>
     * 默认使用SpEL (Spring表达式语言)
     * 表达式的计算上下文为方法的参数。例如: {@code "#userId"} 或 {@code "#request.getRemoteAddr()"}。
     * 如果为空，将根据方法签名生成一个默认的签名。
     */
    String value() default "";

    /**
     * 防重复提交的过期时间，单位为秒，默认3秒
     * <p>
     * 在此时间窗口内，相同的签名将被认为是重复提交
     */
    int timeout() default 3;

    /**
     * 签名处理器
     * 注:需要将指定的处理器类注入到Spring容器中
     */
    Class<? extends KeyResolver> keyResolver() default KeyResolver.class;

    /**
     * 重复提交处理器
     * 注:需要将指定的处理器类注入到Spring容器中
     */
    Class<? extends RepeatSubmitHandler> handler() default RepeatSubmitHandler.class;

    /**
     * 重复提交时的错误消息
     */
    String message() default "请勿重复提交，请稍后再试";

}

