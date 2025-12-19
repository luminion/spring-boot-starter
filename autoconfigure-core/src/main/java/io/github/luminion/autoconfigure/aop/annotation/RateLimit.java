package io.github.luminion.autoconfigure.aop.annotation;

import io.github.luminion.autoconfigure.aop.spi.RateLimiter;
import io.github.luminion.autoconfigure.aop.spi.SignatureProvider;

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
     * 用于计算签名的表达式。指定的{@link #signatureProvider()}将根据此处指定的表达式计算一个唯一签名。
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
     * 周期内的访问次数, 默认1次
     */
    int count() default 1;

    /**
     * 签名处理器
     * 注:需要将指定的处理器类注入到Spring容器中
     */
    Class<? extends SignatureProvider> signatureProvider() default SignatureProvider.class;

    /**
     * 速率限制器
     * 注:需要将指定的限制器类注入到Spring容器中
     */
    Class<? extends RateLimiter> rateLimiter() default RateLimiter.class;


}
