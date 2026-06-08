package io.github.luminion.velo.feign;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogSource;
import io.github.luminion.velo.log.InvocationLogSupport;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.trace.TraceContext;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * Feign 调用日志切面。
 */
@Aspect
public class FeignLogAspect {

    private final VeloProperties properties;

    private final RuntimeJsonSerializer runtimeJsonSerializer;

    private final InvocationLogWriter invocationLogWriter;

    public FeignLogAspect(VeloProperties properties, RuntimeJsonSerializer runtimeJsonSerializer,
            InvocationLogWriter invocationLogWriter) {
        this.properties = properties;
        this.runtimeJsonSerializer = runtimeJsonSerializer;
        this.invocationLogWriter = invocationLogWriter;
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
        String clientAddress = FeignClientMetadataResolver.resolveClientAddress(feignType);
        FeignRequestMetadata requestMetadata = FeignClientMetadataResolver.resolveRequestMetadata(method);
        String target = FeignLogSupport.buildInvocationTarget(clientAddress, method, requestMetadata);
        VeloProperties.InvocationProperties invocationProperties = properties.getLog().getInvocation();
        String argsText = InvocationLogSupport.buildArgsText(signature, joinPoint.getTarget(), joinPoint.getArgs(),
                runtimeJsonSerializer, invocationProperties);
        String mdcKey = properties.getLog().getTrace().getMdcKey();
        boolean createdTraceId = ensureTraceId(mdcKey);
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            InvocationLogRecord record = buildRecord(target, argsText, InvocationLogSupport.buildResultText(result,
                    runtimeJsonSerializer, invocationProperties), InvocationLogSupport.elapsedMs(start), null);
            invocationLogWriter.write(record);
            return result;
        } catch (Throwable ex) {
            InvocationLogRecord record = buildRecord(target, argsText, null, InvocationLogSupport.elapsedMs(start), ex);
            invocationLogWriter.write(record);
            throw ex;
        } finally {
            if (createdTraceId) {
                TraceContext.remove(mdcKey);
            }
        }
    }

    private boolean ensureTraceId(String mdcKey) {
        if (!properties.getLog().getTrace().isEnabled() || StringUtils.hasText(TraceContext.get(mdcKey))) {
            return false;
        }
        TraceContext.put(mdcKey, TraceContext.createTraceId());
        return true;
    }

    private InvocationLogRecord buildRecord(String target, String argsText, String resultText, long costMs,
            Throwable error) {
        InvocationLogRecord record = new InvocationLogRecord();
        record.setTraceId(TraceContext.get(properties.getLog().getTrace().getMdcKey()));
        record.setSource(InvocationLogSource.FEIGN);
        record.setTarget(target);
        record.setCostMs(costMs);
        record.setArgs(argsText);
        record.setSuccess(error == null);
        if (error == null) {
            record.setResult(resultText);
        } else {
            record.setError(error);
            record.setErrorClass(error.getClass().getName());
            record.setErrorMessage(error.getMessage());
        }
        return record;
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
