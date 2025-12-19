package io.github.luminion.autoconfigure.aop.aspectj;

import io.github.luminion.autoconfigure.aop.annotation.RateLimit;
import io.github.luminion.autoconfigure.aop.exception.RateLimitException;
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
    private final BeanFactory beanFactory;

    @Before("@annotation(rateLimit)")
    public void around(JoinPoint joinPoint, RateLimit rateLimit) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        String springExpression = rateLimit.value();
        String signature = beanFactory.getBean(rateLimit.signatureProvider())
                .signature(joinPoint.getTarget(), method, joinPoint.getArgs(), springExpression);
        boolean b = beanFactory.getBean(rateLimit.rateLimiter())
                .doLimit(signature, rateLimit);
        if (!b) {
            String errorMessage = String.format(
                    "Method call frequency has exceeded the limit: no more than %d requests within %d seconds are allowed.",
                    rateLimit.count(),
                    rateLimit.seconds()
            );
            throw new RateLimitException(errorMessage, rateLimit.seconds(), rateLimit.count());
        }
    }

}