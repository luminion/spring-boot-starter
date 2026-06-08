package io.github.luminion.velo.log;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import io.github.luminion.velo.util.InvocationUtils;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Shared helpers for unified invocation logging.
 */
public final class InvocationLogSupport {

    public static final String EMPTY_PAYLOAD = "-";

    public static final String MASKED_PAYLOAD = "******";

    private static final String CIRCULAR_PAYLOAD = "[circular]";

    private static final int MAX_SANITIZE_DEPTH = 8;

    private InvocationLogSupport() {
    }

    public static String buildArgsText(MethodSignature signature, Object target, Object[] args,
            RuntimeJsonSerializer runtimeJsonSerializer, VeloProperties.InvocationProperties properties) {
        if (!properties.isIncludeArgs()) {
            return EMPTY_PAYLOAD;
        }
        Map<String, Object> argumentMap = buildArgumentMap(signature, target, args);
        Pattern sensitivePattern = compileSensitivePattern(properties.getSensitivePattern());
        Map<String, Object> sanitizedArgumentMap = sanitizeMap(argumentMap, sensitivePattern, new IdentityHashMap<>(), 0);
        return normalizePayload(limit(runtimeJsonSerializer.toJson(sanitizedArgumentMap),
                properties.getMaxPayloadLength()));
    }

    public static String buildResultText(Object result, RuntimeJsonSerializer runtimeJsonSerializer,
            VeloProperties.InvocationProperties properties) {
        if (!properties.isIncludeResult()) {
            return EMPTY_PAYLOAD;
        }
        Object resultBody = result instanceof ResponseEntity<?> ? ((ResponseEntity<?>) result).getBody() : result;
        return normalizePayload(limit(runtimeJsonSerializer.toJson(resultBody), properties.getMaxPayloadLength()));
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
        return (System.nanoTime() - startNs) / 1_000_000;
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

    private static Pattern compileSensitivePattern(String patternStr) {
        if (patternStr == null || patternStr.isEmpty()) {
            return null;
        }
        return Pattern.compile(patternStr);
    }

    private static Map<String, Object> sanitizeMap(Map<String, Object> argumentMap,
            Pattern sensitivePattern, IdentityHashMap<Object, Boolean> visited, int depth) {
        Map<String, Object> sanitizedMap = new LinkedHashMap<>();
        if (argumentMap.isEmpty() || sensitivePattern == null || depth > MAX_SANITIZE_DEPTH) {
            sanitizedMap.putAll(argumentMap);
            return sanitizedMap;
        }
        if (visited.containsKey(argumentMap)) {
            sanitizedMap.put("value", CIRCULAR_PAYLOAD);
            return sanitizedMap;
        }
        visited.put(argumentMap, Boolean.TRUE);
        for (Map.Entry<String, Object> entry : argumentMap.entrySet()) {
            sanitizedMap.put(entry.getKey(), sanitizeValue(entry.getKey(), entry.getValue(), sensitivePattern, visited,
                    depth + 1));
        }
        visited.remove(argumentMap);
        return sanitizedMap;
    }

    private static Object sanitizeValue(Object key, Object value, Pattern sensitivePattern,
            IdentityHashMap<Object, Boolean> visited, int depth) {
        if (isSensitiveField(key == null ? null : String.valueOf(key), sensitivePattern)) {
            return MASKED_PAYLOAD;
        }
        if (value == null || depth > MAX_SANITIZE_DEPTH) {
            return value;
        }
        if (value instanceof Map<?, ?>) {
            if (visited.containsKey(value)) {
                return CIRCULAR_PAYLOAD;
            }
            visited.put(value, Boolean.TRUE);
            Map<?, ?> source = (Map<?, ?>) value;
            Map<Object, Object> sanitizedMap = new LinkedHashMap<>(source.size());
            for (Map.Entry<?, ?> entry : source.entrySet()) {
                sanitizedMap.put(entry.getKey(), sanitizeValue(entry.getKey(), entry.getValue(), sensitivePattern, visited,
                        depth + 1));
            }
            visited.remove(value);
            return sanitizedMap;
        }
        if (value instanceof Collection<?>) {
            if (visited.containsKey(value)) {
                return CIRCULAR_PAYLOAD;
            }
            visited.put(value, Boolean.TRUE);
            Collection<?> source = (Collection<?>) value;
            List<Object> sanitizedValues = new ArrayList<>(source.size());
            for (Object item : source) {
                sanitizedValues.add(sanitizeValue(null, item, sensitivePattern, visited, depth + 1));
            }
            visited.remove(value);
            return sanitizedValues;
        }
        if (value.getClass().isArray()) {
            if (visited.containsKey(value)) {
                return CIRCULAR_PAYLOAD;
            }
            visited.put(value, Boolean.TRUE);
            int length = java.lang.reflect.Array.getLength(value);
            List<Object> sanitizedValues = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                sanitizedValues.add(sanitizeValue(null, java.lang.reflect.Array.get(value, i), sensitivePattern, visited,
                        depth + 1));
            }
            visited.remove(value);
            return sanitizedValues;
        }
        return value;
    }

    private static boolean isSensitiveField(String key, Pattern sensitivePattern) {
        if (key == null || key.isEmpty() || sensitivePattern == null) {
            return false;
        }
        return sensitivePattern.matcher(key).find();
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
