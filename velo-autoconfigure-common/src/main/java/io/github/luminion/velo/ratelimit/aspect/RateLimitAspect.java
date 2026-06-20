package io.github.luminion.velo.ratelimit.aspect;

import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.util.ConcurrencyAnnotationUtils;
import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.ratelimit.annotation.RateLimit;
import io.github.luminion.velo.ratelimit.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * 限流切面
 *
 * @author luminion
 */
@Aspect
@RequiredArgsConstructor
public class RateLimitAspect implements Ordered {
    private final String prefix;
    private final Fingerprinter fingerprinter;
    private final RateLimitHandler rateLimitHandler;

    private int order = VeloAdvisorOrder.CONCURRENCY_RATE_LIMIT;

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Around("@annotation(rateLimit)")
    public Object doRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        return applyRateLimit(joinPoint, rateLimit);
    }

    @Around("!@annotation(io.github.luminion.velo.ratelimit.annotation.RateLimit) && @within(rateLimit)")
    public Object doRateLimitOnClass(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        return applyRateLimit(joinPoint, rateLimit);
    }

    private Object applyRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = ConcurrencyAnnotationUtils.resolveSpecificMethod(joinPoint.getTarget(), signature.getMethod());

        double permits = rateLimit.permits();
        long ttl = rateLimit.ttl();

        String methodFingerprint = fingerprinter.resolveMethodFingerprint(
                joinPoint.getTarget(),
                method,
                joinPoint.getArgs(),
                "");
        String keyFingerprint = methodFingerprint;
        if (StringUtils.hasText(rateLimit.key())) {
            keyFingerprint += ':' + fingerprinter.resolveMethodFingerprint(
                    joinPoint.getTarget(),
                    method,
                    joinPoint.getArgs(),
                    rateLimit.key());
        }

        // 1. 生成基础 Key
        String key = ConcurrencyAnnotationUtils.buildPrefixedKey(prefix, keyFingerprint);

        // 2. 执行限流
        if (!rateLimitHandler.tryAcquire(key, permits, ttl, rateLimit.unit())) {
            throw new RateLimitException(rateLimit.message());
        }

        return joinPoint.proceed();
    }
}
