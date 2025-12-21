package io.github.luminion.autoconfigure.ratelimit.spi;

import io.github.luminion.autoconfigure.ratelimit.annotation.RateLimit;

/**
 * 方法限流器。
 *
 * @author luminion
 */
@FunctionalInterface
public interface RateLimiter {

    /**
     * 执行限流
     *
     * @param signature 唯一签名
     * @param rateLimit 方法限流注解
     * @return 是否限流
     */
    boolean tryAccess(String signature, RateLimit rateLimit);

}
