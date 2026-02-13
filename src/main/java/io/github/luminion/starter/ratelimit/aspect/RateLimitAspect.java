package io.github.luminion.starter.ratelimit.aspect;

import io.github.luminion.starter.core.spi.MethodFingerprinter;
import io.github.luminion.starter.ratelimit.annotation.RateLimit;
import io.github.luminion.starter.ratelimit.exception.RateLimitException;
import io.github.luminion.starter.ratelimit.spi.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * @author luminion
 */
@Aspect
@RequiredArgsConstructor
public class RateLimitAspect {
    private final MethodFingerprinter methodFingerprinter;
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
        String signature = methodFingerprinter.resolveMethodFingerprint(joinPoint.getTarget(), method, joinPoint.getArgs(), springExpression);
        boolean b = rateLimiter.tryAccess(signature, rateLimit);
        if (!b) {
            throw new RateLimitException(rateLimit.message());
        }
    }

}