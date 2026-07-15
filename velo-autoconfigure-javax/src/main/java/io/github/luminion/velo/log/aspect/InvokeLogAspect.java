package io.github.luminion.velo.log.aspect;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogSource;
import io.github.luminion.velo.log.InvocationLogSupport;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.InvocationPhase;
import io.github.luminion.velo.log.annotation.LogPayloadIgnore;
import io.github.luminion.velo.log.trace.TraceContext;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import io.github.luminion.velo.spi.provider.HttpMessageConverterRuntimeJsonSerializer;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;

import java.util.Collections;

/**
 * Unified method invocation log aspect.
 *
 * <p>每次调用写两条记录：进入时写 {@link InvocationPhase#ENTRY}（含入参），
 * 退出时写 {@link InvocationPhase#EXIT}（含耗时与返回值或异常）。
 * 慢日志由 {@code SlowLogAspect} 独立处理，本切面不再读取 {@code @SlowLog}。</p>
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

        // 进入日志：记录方法名与入参
        InvocationLogSupport.safeWrite(invocationLogWriter, buildEntryRecord(signature, argsText));

        long start = System.nanoTime();
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            long elapsedNanos = InvocationLogSupport.elapsedNanos(start);
            InvocationLogRecord exitRecord = buildExitRecord(signature,
                    null, InvocationLogSupport.nanosToMillis(elapsedNanos), ex);
            InvocationLogSupport.safeWrite(invocationLogWriter, exitRecord);
            throw ex;
        }

        long elapsedNanos = InvocationLogSupport.elapsedNanos(start);
        InvocationLogRecord exitRecord = buildExitRecord(signature,
                ignoreResult ? InvocationLogSupport.EMPTY_PAYLOAD
                        : InvocationLogSupport.safeBuildResultText(result, runtimeJsonSerializer, invocationProperties),
                InvocationLogSupport.nanosToMillis(elapsedNanos), null);
        InvocationLogSupport.safeWrite(invocationLogWriter, exitRecord);
        return result;
    }

    private InvocationLogRecord buildEntryRecord(MethodSignature signature, String argsText) {
        InvocationLogRecord record = new InvocationLogRecord();
        Class<?> declaringType = signature.getDeclaringType();
        record.setLoggerName(declaringType != null ? declaringType.getName() : null);
        record.setTraceId(TraceContext.get(properties.getLog().getTrace().getMdcKey()));
        record.setSource(InvocationLogSource.INVOKE);
        record.setTarget(signature.getName() + "()");
        record.setPhase(InvocationPhase.ENTRY);
        record.setArgs(argsText);
        record.setSuccess(true);
        return record;
    }

    private InvocationLogRecord buildExitRecord(MethodSignature signature, String resultText,
            long costMs, Throwable error) {
        InvocationLogRecord record = new InvocationLogRecord();
        Class<?> declaringType = signature.getDeclaringType();
        record.setLoggerName(declaringType != null ? declaringType.getName() : null);
        record.setTraceId(TraceContext.get(properties.getLog().getTrace().getMdcKey()));
        record.setSource(InvocationLogSource.INVOKE);
        record.setTarget(signature.getName() + "()");
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

    private RuntimeJsonSerializer runtimeJsonSerializer() {
        return runtimeJsonSerializerProvider.getIfAvailable(
                () -> new HttpMessageConverterRuntimeJsonSerializer(Collections.emptyList()));
    }
}
