package io.github.luminion.velo.log.aspect;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogSource;
import io.github.luminion.velo.log.InvocationLogSupport;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.annotation.LogPayloadIgnore;
import io.github.luminion.velo.log.annotation.SlowLog;
import io.github.luminion.velo.log.trace.TraceContext;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import io.github.luminion.velo.spi.provider.HttpMessageConverterRuntimeJsonSerializer;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.Ordered;

import java.util.Collections;

/**
 * Unified method invocation log aspect.
 */
@Aspect
@RequiredArgsConstructor
public class InvokeLogAspect implements Ordered {

    private final VeloProperties properties;

    private final ObjectProvider<RuntimeJsonSerializer> runtimeJsonSerializerProvider;

    private final InvocationLogWriter invocationLogWriter;

    private int order = VeloAdvisorOrder.LOG_INVOKE;

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Around("@within(io.github.luminion.velo.log.annotation.InvokeLog) " +
            "|| @annotation(io.github.luminion.velo.log.annotation.InvokeLog)")
    public Object logInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RuntimeJsonSerializer runtimeJsonSerializer = runtimeJsonSerializer();
        VeloProperties.InvocationProperties invocationProperties = properties.getLog().getInvocation();
        LogPayloadIgnore logPayloadIgnore = InvocationLogSupport.findLogPayloadIgnore(signature);
        boolean ignoreArgs = logPayloadIgnore != null && logPayloadIgnore.args();
        boolean ignoreResult = logPayloadIgnore != null && logPayloadIgnore.result();
        String argsText = ignoreArgs ? InvocationLogSupport.EMPTY_PAYLOAD
                : InvocationLogSupport.safeBuildArgsText(signature, joinPoint.getTarget(), joinPoint.getArgs(),
                        runtimeJsonSerializer, invocationProperties);
        long start = System.nanoTime();
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            long elapsedNanos = InvocationLogSupport.elapsedNanos(start);
            InvocationLogRecord record = buildRecord(signature, argsText, null,
                    InvocationLogSupport.nanosToMillis(elapsedNanos), ex);
            record.setSlow(isSlow(signature, elapsedNanos));
            InvocationLogSupport.safeWrite(invocationLogWriter, record);
            throw ex;
        }

        long elapsedNanos = InvocationLogSupport.elapsedNanos(start);
        InvocationLogRecord record = buildRecord(signature, argsText,
                ignoreResult ? InvocationLogSupport.EMPTY_PAYLOAD
                        : InvocationLogSupport.safeBuildResultText(result, runtimeJsonSerializer, invocationProperties),
                InvocationLogSupport.nanosToMillis(elapsedNanos), null);
        record.setSlow(isSlow(signature, elapsedNanos));
        InvocationLogSupport.safeWrite(invocationLogWriter, record);
        return result;
    }

    private InvocationLogRecord buildRecord(MethodSignature signature, String argsText, String resultText, long costMs,
            Throwable error) {
        InvocationLogRecord record = new InvocationLogRecord();
        Class<?> declaringType = signature.getDeclaringType();
        record.setLoggerName(declaringType != null ? declaringType.getName() : null);
        record.setTraceId(TraceContext.get(properties.getLog().getTrace().getMdcKey()));
        record.setSource(InvocationLogSource.INVOKE);
        record.setTarget(signature.getName() + "()");
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

    private boolean isSlow(MethodSignature signature, long elapsedNanos) {
        SlowLog slowLog = findSlowLog(signature);
        return slowLog != null
                && InvocationLogSupport.exceedsSlowThresholdNanos(elapsedNanos, slowLog.value(), slowLog.timeUnit());
    }

    private SlowLog findSlowLog(MethodSignature signature) {
        SlowLog slowLog = AnnotatedElementUtils.findMergedAnnotation(signature.getMethod(), SlowLog.class);
        if (slowLog == null) {
            slowLog = AnnotatedElementUtils.findMergedAnnotation(signature.getDeclaringType(), SlowLog.class);
        }
        return slowLog;
    }

    private RuntimeJsonSerializer runtimeJsonSerializer() {
        return runtimeJsonSerializerProvider.getIfAvailable(
                () -> new HttpMessageConverterRuntimeJsonSerializer(Collections.emptyList()));
    }
}
