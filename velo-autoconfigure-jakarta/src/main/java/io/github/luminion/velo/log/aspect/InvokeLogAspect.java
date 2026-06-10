package io.github.luminion.velo.log.aspect;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogSource;
import io.github.luminion.velo.log.InvocationLogSupport;
import io.github.luminion.velo.log.InvocationLogWriter;
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

import java.util.Collections;

/**
 * Unified method invocation log aspect.
 */
@Aspect
@RequiredArgsConstructor
public class InvokeLogAspect {

    private final VeloProperties properties;

    private final ObjectProvider<RuntimeJsonSerializer> runtimeJsonSerializerProvider;

    private final InvocationLogWriter invocationLogWriter;

    @Around("@within(io.github.luminion.velo.log.annotation.InvokeLog) " +
            "|| @annotation(io.github.luminion.velo.log.annotation.InvokeLog)")
    public Object logInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RuntimeJsonSerializer runtimeJsonSerializer = runtimeJsonSerializer();
        VeloProperties.InvocationProperties invocationProperties = properties.getLog().getInvocation();
        String argsText = InvocationLogSupport.buildArgsText(signature, joinPoint.getTarget(), joinPoint.getArgs(),
                runtimeJsonSerializer, invocationProperties);
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            long elapsedMs = InvocationLogSupport.elapsedMs(start);
            InvocationLogRecord record = buildRecord(signature, argsText,
                    InvocationLogSupport.buildResultText(result, runtimeJsonSerializer, invocationProperties),
                    elapsedMs, null);
            record.setSlow(isSlow(signature, elapsedMs));
            invocationLogWriter.write(record);
            return result;
        } catch (Throwable ex) {
            long elapsedMs = InvocationLogSupport.elapsedMs(start);
            InvocationLogRecord record = buildRecord(signature, argsText, null, elapsedMs, ex);
            record.setSlow(isSlow(signature, elapsedMs));
            invocationLogWriter.write(record);
            throw ex;
        }
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

    private boolean isSlow(MethodSignature signature, long elapsedMs) {
        SlowLog slowLog = findSlowLog(signature);
        return slowLog != null && elapsedMs * 1_000_000 > slowLog.timeUnit().toNanos(slowLog.value());
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
