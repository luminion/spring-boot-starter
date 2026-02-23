package io.github.luminion.starter.ratelimit.aspect;

import io.github.luminion.starter.core.fingerprint.MethodFingerprinter;
import io.github.luminion.starter.ratelimit.RateLimitHandler;
import io.github.luminion.starter.ratelimit.annotation.RateLimit;
import io.github.luminion.starter.ratelimit.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
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
    private final MethodFingerprinter methodFingerprinter;
    private final RateLimitHandler rateLimitHandler;

    @Before("@within(io.github.luminion.starter.ratelimit.annotation.RateLimit) || @annotation(io.github.luminion.starter.ratelimit.annotation.RateLimit)")
    public void doRateLimit(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 支持方法级覆盖类级配置
        RateLimit rateLimit = AnnotatedElementUtils.findMergedAnnotation(method, RateLimit.class);
        if (rateLimit == null) {
            rateLimit = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), RateLimit.class);
        }

        if (rateLimit == null) {
            return;
        }

        // 1. 生成基础 Key
        String key = prefix + ":" + methodFingerprinter.resolveMethodFingerprint(
                joinPoint.getTarget(),
                method,
                joinPoint.getArgs(),
                rateLimit.key());

        // 2. 执行限流
        if (!rateLimitHandler.tryAcquire(key, rateLimit.value())) {
            throw new RateLimitException(rateLimit.message());
        }
    }
}