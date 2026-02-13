package io.github.luminion.starter.core.aspect;

import io.github.luminion.starter.core.annotation.RateLimit;
import io.github.luminion.starter.core.spi.MethodFingerprinter;
import io.github.luminion.starter.core.spi.RateLimiter;
import io.github.luminion.starter.core.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * 限流切面
 *
 * @author luminion
 */
@Aspect
@RequiredArgsConstructor
public class RateLimitAspect {

    private final MethodFingerprinter methodFingerprinter;
    private final RateLimiter rateLimiter;

    @Before("@annotation(rateLimit)")
    public void doRateLimit(JoinPoint joinPoint, RateLimit rateLimit) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 1. 生成基础 Key
        String key = generateKey(joinPoint, method, rateLimit);

        // 2. 确定速率和桶容量
        double rate = rateLimit.rate();
        double burst = rateLimit.burst() > 0 ? rateLimit.burst() : rate;

        // 3. 执行限流
        if (!rateLimiter.tryAcquire(key, rate, burst)) {
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
                // 这里可以根据项目权限框架获取用户 ID，此处仅提供占位或通过异常处理
                keyBuilder.append("user:").append(getUserId()).append(":");
                break;
            case GLOBAL:
                keyBuilder.append("global:");
                break;
            default:
                // DEFAULT 模式下不加特殊前缀
                break;
        }

        // 使用指纹解析器解析具体的方法/SpEL 签名
        String fingerprint = methodFingerprinter.resolveMethodFingerprint(
                joinPoint.getTarget(),
                method,
                joinPoint.getArgs(),
                rateLimit.value());

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
        // 最佳实践：用户可以重写此方法或通过注入 UserProvider 获取
        return "anonymous";
    }
}