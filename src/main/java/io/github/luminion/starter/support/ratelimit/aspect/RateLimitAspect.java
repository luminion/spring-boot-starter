package io.github.luminion.starter.support.ratelimit.aspect;

import io.github.luminion.starter.core.spi.KeyResolver;
import io.github.luminion.starter.support.ratelimit.annotation.RateLimit;
import io.github.luminion.starter.support.ratelimit.exception.RateLimitException;
import io.github.luminion.starter.support.ratelimit.spi.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.BeanFactory;

import java.lang.reflect.Method;

/**
 * @author luminion
 */
@Aspect
@RequiredArgsConstructor
public class RateLimitAspect {
    private final KeyResolver keyResolver;
    private final RateLimiter rateLimiter;

    /**
     * 限流
     *
     * @param joinPoint 连接点
     * @param rateLimit 速率限制
     */
    @Before("@annotation(rateLimit)")
    public void doRateLimit(JoinPoint joinPoint, RateLimit rateLimit) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        String springExpression = rateLimit.value();
        String signature = keyResolver.resolve(joinPoint.getTarget(), method, joinPoint.getArgs(), springExpression);
        boolean b = rateLimiter.tryAccess(signature, rateLimit);
        if (!b) {
            throw new RateLimitException(rateLimit.message());
        }
    }

}