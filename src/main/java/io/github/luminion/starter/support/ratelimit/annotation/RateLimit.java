package io.github.luminion.starter.support.ratelimit.annotation;

import io.github.luminion.starter.support.ratelimit.spi.RateLimiter;
import io.github.luminion.starter.core.spi.KeyResolver;

import java.lang.annotation.*;

/**
 * 方法限流
 *
 * @author luminion
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 用于计算签名的表达式。指定的{@link #methodFingerprinter()}将根据此处指定的表达式计算一个唯一签名。
     * <p>
     * 默认使用SpEL (Spring表达式语言)
     * 表达式的计算上下文为方法的参数。例如: {@code "#userId"} 或 {@code "#request.getRemoteAddr()"}。
     * 如果为空，将根据方法签名生成一个默认的签名。
     */
    String value() default "";

    /**
     * 限流周期, 单位为秒, 默认1秒
     */
    int seconds() default 1;
    
    /**
     * 提示信息
     */
    String message() default "Method call frequency has exceeded the limit";

}
