package io.github.luminion.velo.log;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.annotation.LogPayloadIgnore;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import io.github.luminion.velo.util.InvocationUtils;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Shared helpers for unified invocation logging.
 */
public final class InvocationLogSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvocationLogSupport.class);

    public static final String EMPTY_PAYLOAD = "-";

    private InvocationLogSupport() {
    }

    /**
     * Resolves the {@link LogPayloadIgnore} annotation from the method or declaring
     * class of the given signature, supporting meta-annotations and class-level
     * inheritance.
     *
     * @param signature the join point method signature
     * @return the merged {@link LogPayloadIgnore} annotation, or {@code null} if absent
     */
    public static LogPayloadIgnore findLogPayloadIgnore(MethodSignature signature) {
        Method method = signature.getMethod();
        if (method != null) {
            LogPayloadIgnore logPayloadIgnore = AnnotatedElementUtils.findMergedAnnotation(method, LogPayloadIgnore.class);
            if (logPayloadIgnore != null) {
                return logPayloadIgnore;
            }
        }
        Class<?> declaringType = signature.getDeclaringType();
        if (declaringType != null) {
            return AnnotatedElementUtils.findMergedAnnotation(declaringType, LogPayloadIgnore.class);
        }
        return null;
    }

    public static String buildArgsText(MethodSignature signature, Object target, Object[] args,
            RuntimeJsonSerializer runtimeJsonSerializer, VeloProperties.InvocationProperties properties) {
        if (!properties.isIncludeArgs()) {
            return EMPTY_PAYLOAD;
        }
        Map<String, Object> argumentMap = buildArgumentMap(signature, target, args);
        return normalizePayload(limit(runtimeJsonSerializer.toJson(argumentMap),
                properties.getMaxPayloadLength()));
    }

    public static String safeBuildArgsText(MethodSignature signature, Object target, Object[] args,
            RuntimeJsonSerializer runtimeJsonSerializer, VeloProperties.InvocationProperties properties) {
        try {
            return buildArgsText(signature, target, args, runtimeJsonSerializer, properties);
        } catch (RuntimeException ex) {
            LOGGER.warn("Invocation argument serialization failed, payload omitted: {}", ex.toString());
            return EMPTY_PAYLOAD;
        }
    }

    public static String buildResultText(Object result, RuntimeJsonSerializer runtimeJsonSerializer,
            VeloProperties.InvocationProperties properties) {
        if (!properties.isIncludeResult()) {
            return EMPTY_PAYLOAD;
        }
        Object resultBody = result instanceof ResponseEntity<?> ? ((ResponseEntity<?>) result).getBody() : result;
        return normalizePayload(limit(runtimeJsonSerializer.toJson(resultBody), properties.getMaxPayloadLength()));
    }

    public static String safeBuildResultText(Object result, RuntimeJsonSerializer runtimeJsonSerializer,
            VeloProperties.InvocationProperties properties) {
        try {
            return buildResultText(result, runtimeJsonSerializer, properties);
        } catch (RuntimeException ex) {
            LOGGER.warn("Invocation result serialization failed, payload omitted: {}", ex.toString());
            return EMPTY_PAYLOAD;
        }
    }

    public static void safeWrite(InvocationLogWriter invocationLogWriter, InvocationLogRecord record) {
        try {
            invocationLogWriter.write(record);
        } catch (RuntimeException ex) {
            LOGGER.warn("Invocation log writer failed for {}, record dropped: {}", record.getTarget(), ex.toString());
        }
    }

    public static String normalizePayload(String text) {
        if (text == null || "null".equals(text)) {
            return EMPTY_PAYLOAD;
        }
        return text;
    }

    public static String errorSummary(Throwable error) {
        if (error == null) {
            return EMPTY_PAYLOAD;
        }
        String message = error.getMessage();
        if (StringUtils.hasText(message)) {
            return error.getClass().getSimpleName() + ": " + message;
        }
        return error.getClass().getSimpleName();
    }

    public static long elapsedMs(long startNs) {
        return TimeUnit.NANOSECONDS.toMillis(elapsedNanos(startNs));
    }

    public static long elapsedNanos(long startNs) {
        return System.nanoTime() - startNs;
    }

    public static long nanosToMillis(long elapsedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
    }

    /**
     * Compares a monotonic nanosecond duration against a millisecond threshold.
     */
    public static boolean exceedsSlowThresholdNanos(long elapsedNanos, long threshold) {
        return elapsedNanos > TimeUnit.MILLISECONDS.toNanos(threshold);
    }

    private static Map<String, Object> buildArgumentMap(MethodSignature signature, Object target, Object[] args) {
        Map<String, Object> argumentMap = new LinkedHashMap<>();
        if (args == null || args.length == 0) {
            return argumentMap;
        }
        String[] parameterNames = InvocationUtils.resolveParameterNames(signature, target);
        for (int i = 0; i < args.length; i++) {
            String parameterName = parameterNames != null && i < parameterNames.length ? parameterNames[i] : "arg" + i;
            argumentMap.put(parameterName, args[i]);
        }
        return argumentMap;
    }

    private static String limit(String text, int maxPayloadLength) {
        if (maxPayloadLength == 0) {
            return EMPTY_PAYLOAD;
        }
        if (maxPayloadLength < 0) {
            return text;
        }
        int safeMaxPayloadLength = Math.max(0, maxPayloadLength);
        if (text == null || text.length() <= safeMaxPayloadLength) {
            return text;
        }
        if (safeMaxPayloadLength <= 3) {
            return text.substring(0, safeMaxPayloadLength);
        }
        return text.substring(0, safeMaxPayloadLength - 3) + "...";
    }
}
