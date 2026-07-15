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
 * 慢调用日志切面。
 *
 * <p>超过 {@link SlowLog} 阈值时独立打印一条慢日志，级别由
 * {@code velo.log.slow.level} 控制（默认 WARN）。
 * 慢日志与 Controller / Feign / {@code @InvokeLog} 的进出日志相互独立，各自打印，不退避。</p>
 */
@Aspect
@RequiredArgsConstructor
public class SlowLogAspect implements Ordered {

    private final VeloProperties properties;

    private final ObjectProvider<RuntimeJsonSerializer> runtimeJsonSerializerProvider;

    private final InvocationLogWriter invocationLogWriter;

    private int order = VeloAdvisorOrder.LOG_SLOW;

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Around("@within(io.github.luminion.velo.log.annotation.SlowLog) " +
            "|| @annotation(io.github.luminion.velo.log.annotation.SlowLog)")
    public Object logTime(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        SlowLog slowLog = AnnotatedElementUtils.findMergedAnnotation(signature.getMethod(), SlowLog.class);
        if (slowLog == null) {
            slowLog = AnnotatedElementUtils.findMergedAnnotation(signature.getDeclaringType(), SlowLog.class);
        }
        if (slowLog == null) {
            return joinPoint.proceed();
        }

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
            if (InvocationLogSupport.exceedsSlowThresholdNanos(elapsedNanos, slowLog.value(), slowLog.timeUnit())) {
                InvocationLogRecord record = buildRecord(signature, argsText, null,
                        InvocationLogSupport.nanosToMillis(elapsedNanos), ex);
                InvocationLogSupport.safeWrite(invocationLogWriter, record);
            }
            throw ex;
        }

        long elapsedNanos = InvocationLogSupport.elapsedNanos(start);
        if (InvocationLogSupport.exceedsSlowThresholdNanos(elapsedNanos, slowLog.value(), slowLog.timeUnit())) {
            InvocationLogRecord record = buildRecord(signature, argsText,
                    ignoreResult ? InvocationLogSupport.EMPTY_PAYLOAD
                            : InvocationLogSupport.safeBuildResultText(result, runtimeJsonSerializer, invocationProperties),
                    InvocationLogSupport.nanosToMillis(elapsedNanos), null);
            InvocationLogSupport.safeWrite(invocationLogWriter, record);
        }
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
        record.setSlow(true);
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

    private RuntimeJsonSerializer runtimeJsonSerializer() {
        return runtimeJsonSerializerProvider.getIfAvailable(
                () -> new HttpMessageConverterRuntimeJsonSerializer(Collections.emptyList()));
    }
}
