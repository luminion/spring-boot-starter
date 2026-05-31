package io.github.luminion.velo.feign;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import io.github.luminion.velo.util.InvocationUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Feign 调用日志公共支持。
 */
final class FeignLogSupport {

    static final String EMPTY_PAYLOAD = "-";

    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private FeignLogSupport() {
    }

    static String buildArgsText(Method method, Object[] args, RuntimeJsonSerializer runtimeJsonSerializer,
            VeloProperties.FeignProperties properties) {
        return limit(runtimeJsonSerializer.toJson(buildArgumentMap(method, args)), properties.getRequestLoggingMaxPayloadLength());
    }

    static String buildResultText(Object result, RuntimeJsonSerializer runtimeJsonSerializer,
            VeloProperties.FeignProperties properties) {
        Object resultBody = result instanceof ResponseEntity<?> ? ((ResponseEntity<?>) result).getBody() : result;
        String resultText = limit(runtimeJsonSerializer.toJson(resultBody), properties.getRequestLoggingMaxPayloadLength());
        if (resultText == null || "null".equals(resultText)) {
            return EMPTY_PAYLOAD;
        }
        return resultText;
    }

    static String buildInvocationPrefix(String clientName, Method method, FeignRequestMetadata requestMetadata) {
        StringBuilder builder = new StringBuilder("[Feign");
        if (StringUtils.hasText(clientName)) {
            builder.append(' ').append(clientName);
        }
        if (requestMetadata != null && StringUtils.hasText(requestMetadata.getHttpMethod())) {
            builder.append(' ').append(requestMetadata.getHttpMethod());
        }
        if (requestMetadata != null && StringUtils.hasText(requestMetadata.getPath())) {
            builder.append(' ').append(requestMetadata.getPath());
        } else {
            builder.append(' ').append(method.getDeclaringClass().getSimpleName()).append('.').append(method.getName());
        }
        builder.append("] ");
        return builder.toString();
    }

    static boolean isEnabled(org.slf4j.Logger logger, LogLevel level) {
        LogLevel targetLevel = level == null ? LogLevel.INFO : level;
        if (targetLevel == LogLevel.OFF) {
            return false;
        }
        switch (targetLevel) {
            case TRACE:
                return logger.isTraceEnabled();
            case INFO:
                return logger.isInfoEnabled();
            case WARN:
                return logger.isWarnEnabled();
            case ERROR:
            case FATAL:
                return logger.isErrorEnabled();
            default:
                return logger.isDebugEnabled();
        }
    }

    static void log(org.slf4j.Logger logger, LogLevel level, String format, Object... arguments) {
        LogLevel targetLevel = level == null ? LogLevel.INFO : level;
        if (targetLevel == LogLevel.OFF) {
            return;
        }
        switch (targetLevel) {
            case TRACE:
                logger.trace(format, arguments);
                return;
            case INFO:
                logger.info(format, arguments);
                return;
            case WARN:
                logger.warn(format, arguments);
                return;
            case ERROR:
            case FATAL:
                logger.error(format, arguments);
                return;
            default:
                logger.debug(format, arguments);
        }
    }

    private static Map<String, Object> buildArgumentMap(Method method, Object[] args) {
        Map<String, Object> argumentMap = new LinkedHashMap<>();
        if (args == null || args.length == 0) {
            return argumentMap;
        }
        String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);
        for (int i = 0; i < args.length; i++) {
            String parameterName = parameterNames != null && i < parameterNames.length ? parameterNames[i] : "arg" + i;
            argumentMap.put(parameterName, args[i]);
        }
        return argumentMap;
    }

    private static String limit(String text, int maxPayloadLength) {
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
