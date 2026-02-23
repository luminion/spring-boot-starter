package io.github.luminion.starter.ratelimit.aspect;

import io.github.luminion.starter.core.fingerprint.MethodFingerprinter;
import io.github.luminion.starter.ratelimit.RateLimiter;
import io.github.luminion.starter.ratelimit.annotation.RateLimit;
import io.github.luminion.starter.ratelimit.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
    private final RateLimiter rateLimiter;

    @Before("@within(io.github.luminion.starter.ratelimit.annotation.RateLimit) || @annotation(io.github.luminion.starter.ratelimit.annotation.RateLimit)")
    public void doRateLimit(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 支持方法级覆盖类级配置
        RateLimit rateLimit = AnnotatedElementUtils.findMergedAnnotation(method, RateLimit.class);
        if (rateLimit == null) {
            rateLimit = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), RateLimit.class);
        }

        if (rateLimit == null)
            return;

        // 1. 生成基础 Key
        String key = prefix + generateKey(joinPoint, method, rateLimit);

        // 2. 确定速率
        double rate = rateLimit.value();

        // 3. 执行限流
        if (!rateLimiter.tryAcquire(key, rate)) {
            throw new RateLimitException(rateLimit.message());
        }
    }

    private String generateKey(JoinPoint joinPoint, Method method, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder("rate_limit:");

        // 处理不同策略的 Key 前缀
        RateLimit.LimitType type = rateLimit.limitType();
        switch (type) {
            case IP:
                keyBuilder.append(getIpAddress()).append(":");
                break;
            case USER:
                keyBuilder.append("user:").append(getUserId()).append(":");
                break;
            case GLOBAL:
                keyBuilder.append("global:");
                break;
            default:
                break;
        }

        // 使用指纹解析器解析具体的 SpEL 或指纹 (使用 rateLimit.key() 而非之前的 value())
        String fingerprint = methodFingerprinter.resolveMethodFingerprint(
                joinPoint.getTarget(),
                method,
                joinPoint.getArgs(),
                rateLimit.key());

        return keyBuilder.append(fingerprint).toString();
    }

    private String getIpAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null)
            return "unknown";
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String getUserId() {
        return "anonymous";
    }
}