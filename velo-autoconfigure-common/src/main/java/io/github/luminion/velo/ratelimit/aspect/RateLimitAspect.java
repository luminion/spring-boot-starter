package io.github.luminion.velo.ratelimit.aspect;

import io.github.luminion.velo.core.spi.Fingerprinter;
import io.github.luminion.velo.core.util.ConcurrencyAnnotationUtils;
import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.ratelimit.annotation.RateLimit;
import io.github.luminion.velo.ratelimit.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
/**
 * 限流切面
 *
 * @author luminion
 */
@Aspect
@RequiredArgsConstructor
public class RateLimitAspect {
    private final String prefix;
    private final Fingerprinter fingerprinter;
    private final RateLimitHandler rateLimitHandler;

    @Before("@within(io.github.luminion.velo.ratelimit.annotation.RateLimit) || @annotation(io.github.luminion.velo.ratelimit.annotation.RateLimit)")
    public void doRateLimit(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = ConcurrencyAnnotationUtils.resolveSpecificMethod(joinPoint.getTarget(), signature.getMethod());
        Class<?> targetClass = joinPoint.getTarget() != null
                ? AopUtils.getTargetClass(joinPoint.getTarget())
                : method.getDeclaringClass();

        // 支持方法级覆盖类级配置
        RateLimit rateLimit = AnnotatedElementUtils.findMergedAnnotation(method, RateLimit.class);
        if (rateLimit == null) {
            rateLimit = AnnotatedElementUtils.findMergedAnnotation(targetClass, RateLimit.class);
        }

        if (rateLimit == null) {
            return;
        }
        double permits = rateLimit.permits();
        long ttl = rateLimit.ttl();

        // 1. 生成基础 Key
        String key = ConcurrencyAnnotationUtils.buildPrefixedKey(
                prefix,
                fingerprinter.resolveMethodFingerprint(
                        joinPoint.getTarget(),
                        method,
                        joinPoint.getArgs(),
                        rateLimit.key()));

        // 2. 执行限流
        if (!rateLimitHandler.tryAcquire(key, permits, ttl, rateLimit.unit())) {
            throw new RateLimitException(rateLimit.message());
        }
    }
}
