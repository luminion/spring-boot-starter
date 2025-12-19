package io.github.luminion.autoconfigure.aop.spi;

import io.github.luminion.autoconfigure.aop.annotation.RateLimit;

/**
 * 方法限流器。
 *
 * @author luminion
 */
public interface RateLimiter {

    /**
     * 执行限流
     *
     * @param signature 唯一签名
     * @param rateLimit 方法限流注解
     * @return 是否限流
     */
    boolean doLimit(String signature, RateLimit rateLimit);

}
