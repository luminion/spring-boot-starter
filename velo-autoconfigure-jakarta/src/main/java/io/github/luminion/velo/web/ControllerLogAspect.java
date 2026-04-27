package io.github.luminion.velo.web;

import io.github.luminion.velo.core.VeloProperties;
import io.github.luminion.velo.core.spi.RuntimeJsonSerializer;
import io.github.luminion.velo.core.util.WebUtils;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller 调用日志切面。
 */
@Aspect
@RequiredArgsConstructor
public class ControllerLogAspect {

    private final VeloProperties properties;

    private final RuntimeJsonSerializer runtimeJsonSerializer;

    @Around("execution(public * *(..)) && (within(@org.springframework.web.bind.annotation.RestController *) || @annotation(org.springframework.web.bind.annotation.ResponseBody) || @within(org.springframework.web.bind.annotation.ResponseBody))")
    public Object logControllerInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Logger logger = LoggerFactory.getLogger(signature.getDeclaringType());
        VeloProperties.RequestLoggingProperties requestLogging = properties.getWeb().getRequestLogging();
        String requestPrefix = buildRequestPrefix(requestLogging);
        String requestQuery = buildRequestQuery();
        String argsText = requestLogging.isIncludePayload()
                ? limit(runtimeJsonSerializer.toJson(buildArgumentMap(signature, joinPoint.getArgs())),
                requestLogging.getMaxPayloadLength())
                : "[payload-disabled]";

        log(logger, properties.getLogLevel(), "{}{}==> args: {}", requestPrefix, requestQuery, argsText);
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            Object resultBody = result instanceof ResponseEntity<?> ? ((ResponseEntity<?>) result).getBody() : result;
            String resultText = requestLogging.isIncludePayload()
                    ? limit(runtimeJsonSerializer.toJson(resultBody), requestLogging.getMaxPayloadLength())
                    : "[payload-disabled]";
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            log(logger, properties.getLogLevel(), "{}{}<== cost:{}ms, resp: {}", requestPrefix, requestQuery, elapsedMs, resultText);
            return result;
        } catch (Throwable ex) {
            log(logger, LogLevel.ERROR, "{}{}<!! failed: {}", requestPrefix, requestQuery, ex.getMessage(), ex);
            throw ex;
        }
    }

    private Map<String, Object> buildArgumentMap(MethodSignature signature, Object[] args) {
        Map<String, Object> argumentMap = new LinkedHashMap<>();
        if (args == null || args.length == 0) {
            return argumentMap;
        }
        String[] parameterNames = signature.getParameterNames();
        for (int i = 0; i < args.length; i++) {
            String parameterName = parameterNames != null && i < parameterNames.length ? parameterNames[i] : "arg" + i;
            argumentMap.put(parameterName, args[i]);
        }
        return argumentMap;
    }

    private String limit(String text, int maxLength) {
        if (maxLength <= 0 || text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private String buildRequestPrefix(VeloProperties.RequestLoggingProperties requestLogging) {
        if (!WebUtils.isWebContext()) {
            return "";
        }

        return "[" + WebUtils.getRequestIp() + ' ' + WebUtils.getRequestMethod() + ' ' + WebUtils.getRequestURI() + "] ";
    }

    private String buildRequestQuery() {
        if (!WebUtils.isWebContext()) {
            return "";
        }

        String queryString = WebUtils.getRequestQueryString();
        if (queryString == null || queryString.isEmpty()) {
            return "";
        }
        return "query=" + queryString + ' ';
    }

    private void log(Logger logger, LogLevel level, String format, Object... arguments) {
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
}
