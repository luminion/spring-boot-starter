package io.github.luminion.velo.web;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
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

    private static final int MAX_PAYLOAD_LENGTH = 2000;

    private final VeloProperties properties;

    private final RuntimeJsonSerializer runtimeJsonSerializer;

    @Around("execution(public * *(..)) && (within(@org.springframework.web.bind.annotation.RestController *) || @annotation(org.springframework.web.bind.annotation.ResponseBody) || @within(org.springframework.web.bind.annotation.ResponseBody))")
    public Object logControllerInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Logger logger = LoggerFactory.getLogger(signature.getDeclaringType());
        String requestPrefix = buildRequestPrefix();
        String argsText = limit(runtimeJsonSerializer.toJson(buildArgumentMap(signature, joinPoint.getArgs())));

        log(logger, properties.getLog().getLevel(), "{}==> args: {}", requestPrefix, argsText);
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            Object resultBody = result instanceof ResponseEntity<?> ? ((ResponseEntity<?>) result).getBody() : result;
            String resultText = limit(runtimeJsonSerializer.toJson(resultBody));
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            log(logger, properties.getLog().getLevel(), "{}<== cost:{}ms, resp: {}", requestPrefix, elapsedMs, resultText);
            return result;
        } catch (Throwable ex) {
            log(logger, LogLevel.ERROR, "{}<!! failed: {}", requestPrefix, ex.getMessage(), ex);
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

    private String limit(String text) {
        if (text == null || text.length() <= MAX_PAYLOAD_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_PAYLOAD_LENGTH - 3) + "...";
    }

    private String buildRequestPrefix() {
        if (!WebUtils.isWebContext()) {
            return "";
        }

        return "[" + WebUtils.getRequestIp() + ' ' + WebUtils.getRequestMethod() + ' ' + WebUtils.getRequestURI() + "] ";
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
