package io.github.luminion.velo.web;

import io.github.luminion.velo.core.VeloProperties;
import io.github.luminion.velo.core.util.InvocationUtils;
import io.github.luminion.velo.core.util.WebUtils;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;

/**
 * Controller 调用日志切面。
 */
@Aspect
@RequiredArgsConstructor
public class ControllerLogAspect {

    private final VeloProperties properties;

    @Around("execution(public * *(..)) && (within(@org.springframework.web.bind.annotation.RestController *) || @annotation(org.springframework.web.bind.annotation.ResponseBody) || @within(org.springframework.web.bind.annotation.ResponseBody))")
    public Object logControllerInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Logger logger = LoggerFactory.getLogger(signature.getDeclaringType());
        VeloProperties.RequestLoggingProperties requestLogging = properties.getWeb().getRequestLogging();
        String methodName = InvocationUtils.getMethodName(signature);
        String requestPrefix = buildRequestPrefix(requestLogging);
        String argsText = requestLogging.isIncludePayload()
                ? InvocationUtils.formatArguments(signature, joinPoint.getArgs(), requestLogging.getMaxPayloadLength())
                : "[payload-disabled]";

        log(logger, properties.getLogLevel(), "{}==> Controller: {} args={}", requestPrefix, methodName, argsText);
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            String resultText = requestLogging.isIncludePayload()
                    ? InvocationUtils.formatValue(result, requestLogging.getMaxPayloadLength())
                    : "[payload-disabled]";
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            log(logger, properties.getLogLevel(), "{}<== Controller: {} result={} cost={}ms", requestPrefix, methodName,
                    resultText, elapsedMs);
            return result;
        } catch (Throwable ex) {
            log(logger, LogLevel.ERROR, "{}<!! Controller: {} failed: {}", requestPrefix, methodName, ex.getMessage(), ex);
            throw ex;
        }
    }

    private String buildRequestPrefix(VeloProperties.RequestLoggingProperties requestLogging) {
        if (!WebUtils.isWebContext()) {
            return "";
        }

        StringBuilder builder = new StringBuilder("[");
        if (requestLogging.isIncludeClientInfo()) {
            builder.append(WebUtils.getRequestIp()).append(' ');
        }
        builder.append(WebUtils.getRequestMethod()).append(' ').append(WebUtils.getRequestURI());
        if (requestLogging.isIncludeQueryString()) {
            String queryString = WebUtils.getRequestQueryString();
            if (queryString != null && !queryString.isEmpty()) {
                builder.append('?').append(queryString);
            }
        }
        builder.append("] ");
        return builder.toString();
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
