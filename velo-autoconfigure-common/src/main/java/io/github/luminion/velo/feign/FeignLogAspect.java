package io.github.luminion.velo.feign;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogSource;
import io.github.luminion.velo.log.InvocationLogSupport;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.InvocationPhase;
import io.github.luminion.velo.log.annotation.LogPayloadIgnore;
import io.github.luminion.velo.log.trace.TraceContext;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import io.github.luminion.velo.core.VeloAdvisorOrder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import org.springframework.core.Ordered;

/**
 * Feign 调用日志切面。
 *
 * <p>每次调用写两条记录：进入时写 {@link InvocationPhase#ENTRY}（含入参），
 * 退出时写 {@link InvocationPhase#EXIT}（含耗时与返回值或异常）。</p>
 */
@Aspect
public class FeignLogAspect implements Ordered {

    private final VeloProperties properties;

    private final RuntimeJsonSerializer runtimeJsonSerializer;

    private final InvocationLogWriter invocationLogWriter;

    private int order = VeloAdvisorOrder.LOG_FEIGN;

    public FeignLogAspect(VeloProperties properties, RuntimeJsonSerializer runtimeJsonSerializer,
            InvocationLogWriter invocationLogWriter) {
        this.properties = properties;
        this.runtimeJsonSerializer = runtimeJsonSerializer;
        this.invocationLogWriter = invocationLogWriter;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
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
        FeignRequestMetadata requestMetadata = FeignClientMetadataResolver.resolveRequestMetadata(method);
        String target = FeignLogSupport.buildInvocationTarget(method, requestMetadata);
        VeloProperties.InvocationProperties invocationProperties = properties.getLog().getInvocation();
        LogPayloadIgnore logPayloadIgnore = InvocationLogSupport.findLogPayloadIgnore(signature);
        boolean ignoreArgs = logPayloadIgnore != null && logPayloadIgnore.args();
        boolean ignoreResult = logPayloadIgnore != null && logPayloadIgnore.result();
        String argsText = ignoreArgs ? InvocationLogSupport.EMPTY_PAYLOAD
                : InvocationLogSupport.safeBuildArgsText(signature, joinPoint.getTarget(), joinPoint.getArgs(),
                        runtimeJsonSerializer, invocationProperties);
        String mdcKey = properties.getLog().getTrace().getMdcKey();
        boolean createdTraceId = ensureTraceId(mdcKey);

        // 进入日志：记录调用目标与入参
        InvocationLogSupport.safeWrite(invocationLogWriter,
                buildEntryRecord(feignType.getName(), target, argsText));

        long start = System.nanoTime();
        try {
            Object result;
            try {
                result = joinPoint.proceed();
            } catch (Throwable ex) {
                InvocationLogRecord exitRecord = buildExitRecord(feignType.getName(), target,
                        null, InvocationLogSupport.elapsedMs(start), ex);
                InvocationLogSupport.safeWrite(invocationLogWriter, exitRecord);
                throw ex;
            }

            InvocationLogRecord exitRecord = buildExitRecord(feignType.getName(), target,
                    ignoreResult ? InvocationLogSupport.EMPTY_PAYLOAD
                            : InvocationLogSupport.safeBuildResultText(result, runtimeJsonSerializer, invocationProperties),
                    InvocationLogSupport.elapsedMs(start), null);
            InvocationLogSupport.safeWrite(invocationLogWriter, exitRecord);
            return result;
        } finally {
            if (createdTraceId) {
                TraceContext.remove(mdcKey);
            }
        }
    }

    private boolean ensureTraceId(String mdcKey) {
        // mdcKey 未配置时用空 key 操作 MDC 会静默丢失 trace，直接跳过
        if (!StringUtils.hasText(mdcKey) || !properties.getLog().getTrace().isEnabled()
                || StringUtils.hasText(TraceContext.get(mdcKey))) {
            return false;
        }
        TraceContext.put(mdcKey, TraceContext.createTraceId());
        return true;
    }

    private InvocationLogRecord buildEntryRecord(String loggerName, String target, String argsText) {
        InvocationLogRecord record = new InvocationLogRecord();
        record.setLoggerName(loggerName);
        record.setTraceId(TraceContext.get(properties.getLog().getTrace().getMdcKey()));
        record.setSource(InvocationLogSource.FEIGN);
        record.setTarget(target);
        record.setPhase(InvocationPhase.ENTRY);
        record.setArgs(argsText);
        record.setSuccess(true);
        return record;
    }

    private InvocationLogRecord buildExitRecord(String loggerName, String target,
            String resultText, long costMs, Throwable error) {
        InvocationLogRecord record = new InvocationLogRecord();
        record.setLoggerName(loggerName);
        record.setTraceId(TraceContext.get(properties.getLog().getTrace().getMdcKey()));
        record.setSource(InvocationLogSource.FEIGN);
        record.setTarget(target);
        record.setPhase(InvocationPhase.EXIT);
        record.setCostMs(costMs);
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
