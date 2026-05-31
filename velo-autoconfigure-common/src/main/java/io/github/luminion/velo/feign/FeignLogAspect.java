package io.github.luminion.velo.feign;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * Feign 调用日志切面。
 */
@Aspect
public class FeignLogAspect {

    private final VeloProperties properties;

    private final RuntimeJsonSerializer runtimeJsonSerializer;

    public FeignLogAspect(VeloProperties properties, RuntimeJsonSerializer runtimeJsonSerializer) {
        this.properties = properties;
        this.runtimeJsonSerializer = runtimeJsonSerializer;
    }

    @Around("execution(public * *(..)) && (within(@org.springframework.cloud.openfeign.FeignClient *) || @within(org.springframework.cloud.openfeign.FeignClient))")
    public Object logFeignInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        if (ReflectionUtils.isObjectMethod(method)) {
            return joinPoint.proceed();
        }

        Class<?> feignType = resolveFeignType(joinPoint, method);
        if (feignType == null) {
            return joinPoint.proceed();
        }
        Logger logger = LoggerFactory.getLogger(feignType);
        LogLevel level = properties.getLog().getLevel();
        VeloProperties.FeignProperties feignProperties = properties.getFeign();
        String clientName = FeignClientMetadataResolver.resolveClientName(feignType);
        FeignRequestMetadata requestMetadata = FeignClientMetadataResolver.resolveRequestMetadata(method);
        String prefix = FeignLogSupport.buildInvocationPrefix(clientName, method, requestMetadata);

        if (FeignLogSupport.isEnabled(logger, level)) {
            String argsText = FeignLogSupport.buildArgsText(method, joinPoint.getArgs(), runtimeJsonSerializer, feignProperties);
            FeignLogSupport.log(logger, level, "{}==> args: {}", prefix, argsText);
        }
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            if (FeignLogSupport.isEnabled(logger, level)) {
                String resultText = FeignLogSupport.buildResultText(result, runtimeJsonSerializer, feignProperties);
                long elapsedMs = (System.nanoTime() - start) / 1_000_000;
                FeignLogSupport.log(logger, level, "{}<== cost:{}ms, resp: {}", prefix, elapsedMs, resultText);
            }
            return result;
        } catch (Throwable ex) {
            FeignLogSupport.log(logger, LogLevel.ERROR, "{}<!! failed: {}", prefix, ex.getMessage(), ex);
            throw ex;
        }
    }

    private Class<?> resolveFeignType(ProceedingJoinPoint joinPoint, Method method) {
        Class<?> declaringClass = method.getDeclaringClass();
        if (FeignClientMetadataResolver.isFeignClientType(declaringClass)) {
            return declaringClass;
        }
        Object target = joinPoint.getTarget();
        if (target == null) {
            return null;
        }
        Class<?> targetClass = ClassUtils.getUserClass(target);
        if (FeignClientMetadataResolver.isFeignClientType(targetClass)) {
            return targetClass;
        }
        for (Class<?> interfaceType : targetClass.getInterfaces()) {
            if (FeignClientMetadataResolver.isFeignClientType(interfaceType)) {
                return interfaceType;
            }
        }
        return null;
    }
}
