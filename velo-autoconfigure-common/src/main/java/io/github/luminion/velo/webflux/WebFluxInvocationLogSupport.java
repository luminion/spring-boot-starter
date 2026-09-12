package io.github.luminion.velo.webflux;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogSource;
import io.github.luminion.velo.log.InvocationLogSupport;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.InvocationPhase;
import io.github.luminion.velo.log.trace.TraceContext;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import reactor.util.context.ContextView;

import java.util.Collections;

/**
 * WebFlux 方法调用日志公共逻辑。
 *
 * @author luminion
 * @since 1.3.1
 */
final class WebFluxInvocationLogSupport {

    private WebFluxInvocationLogSupport() {
    }

    static RuntimeJsonSerializer resolveSerializer(ObjectProvider<RuntimeJsonSerializer> provider) {
        return provider.getIfAvailable(
                () -> new WebFluxRuntimeJsonSerializer(Collections.emptyList()));
    }

    static String loggerName(MethodSignature signature) {
        return signature.getDeclaringType() == null ? null : signature.getDeclaringType().getName();
    }

    static String traceId(ContextView contextView, VeloProperties properties) {
        String traceId = WebFluxContext.traceId(contextView);
        if (traceId != null) {
            return traceId;
        }
        return TraceContext.get(properties.getLog().getTrace().getMdcKey());
    }

    static String argsText(MethodSignature signature, Object target, Object[] args,
            RuntimeJsonSerializer serializer, VeloProperties.InvocationProperties properties,
            boolean ignoreArgs) {
        return ignoreArgs ? InvocationLogSupport.IGNORED_PAYLOAD
                : InvocationLogSupport.safeBuildArgsText(signature, target, args, serializer, properties);
    }

    static String resultText(MethodSignature signature, Object result, RuntimeJsonSerializer serializer,
            VeloProperties.InvocationProperties properties, boolean ignoreResult) {
        if (signature.getReturnType() == Void.TYPE) {
            return InvocationLogSupport.VOID_RESULT;
        }
        return ignoreResult ? InvocationLogSupport.IGNORED_PAYLOAD
                : InvocationLogSupport.safeBuildResultText(result, serializer, properties);
    }

    static String fluxResultText(VeloProperties.InvocationProperties properties, boolean ignoreResult, long count) {
        if (ignoreResult) {
            return InvocationLogSupport.IGNORED_PAYLOAD;
        }
        if (!properties.isIncludeResult() || properties.getMaxPayloadLength() == 0) {
            return InvocationLogSupport.DISABLED_PAYLOAD;
        }
        String text = "Flux{count=" + count + "}";
        int maxLength = properties.getMaxPayloadLength();
        if (maxLength < 0 || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    static void writeEntry(InvocationLogWriter writer, String loggerName, String target, String traceId,
            String argsText) {
        InvocationLogRecord record = new InvocationLogRecord();
        record.setLoggerName(loggerName);
        record.setTraceId(traceId);
        record.setSource(InvocationLogSource.INVOKE);
        record.setTarget(target);
        record.setPhase(InvocationPhase.ENTRY);
        record.setArgs(argsText);
        record.setSuccess(true);
        InvocationLogSupport.safeWrite(writer, record);
    }

    static void writeExit(InvocationLogWriter writer, String loggerName, String target, String traceId,
            String resultText, long costMs, Throwable error, String argsText) {
        InvocationLogRecord record = new InvocationLogRecord();
        record.setLoggerName(loggerName);
        record.setTraceId(traceId);
        record.setSource(InvocationLogSource.INVOKE);
        record.setTarget(target);
        record.setPhase(InvocationPhase.EXIT);
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
        InvocationLogSupport.safeWrite(writer, record);
    }

    static void writeSlow(InvocationLogWriter writer, String loggerName, String target, String traceId,
            String resultText, long costMs, Throwable error, String argsText, long threshold) {
        InvocationLogRecord record = new InvocationLogRecord();
        record.setLoggerName(loggerName);
        record.setTraceId(traceId);
        record.setSource(InvocationLogSource.INVOKE);
        record.setTarget(target);
        record.setCostMs(costMs);
        record.setArgs(argsText);
        record.setSlow(true);
        record.setSlowThreshold(threshold);
        record.setSuccess(error == null);
        if (error == null) {
            record.setResult(resultText);
        } else {
            record.setError(error);
            record.setErrorClass(error.getClass().getName());
            record.setErrorMessage(error.getMessage());
        }
        InvocationLogSupport.safeWrite(writer, record);
    }
}
