package io.github.luminion.velo.web;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogSource;
import io.github.luminion.velo.log.InvocationLogSupport;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.annotation.LogPayloadIgnore;
import io.github.luminion.velo.log.trace.TraceContext;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import io.github.luminion.velo.core.util.WebUtils;
import io.github.luminion.velo.core.VeloAdvisorOrder;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.core.Ordered;

/**
 * Controller 调用日志切面。
 */
@Aspect
@RequiredArgsConstructor
public class ControllerLogAspect implements Ordered {

    private final VeloProperties properties;

    private final RuntimeJsonSerializer runtimeJsonSerializer;

    private final InvocationLogWriter invocationLogWriter;

    private int order = VeloAdvisorOrder.LOG_CONTROLLER;

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Around("execution(public * *(..)) && (within(@org.springframework.web.bind.annotation.RestController *) || @annotation(org.springframework.web.bind.annotation.ResponseBody) || @within(org.springframework.web.bind.annotation.ResponseBody))")
    public Object logControllerInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> declaringType = signature.getDeclaringType();
        String loggerName = declaringType != null ? declaringType.getName() : null;
        String target = buildRequestTarget();
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
            InvocationLogRecord record = buildRecord(loggerName, target, argsText, null, InvocationLogSupport.elapsedMs(start), ex);
            InvocationLogSupport.safeWrite(invocationLogWriter, record);
            throw ex;
        }

        InvocationLogRecord record = buildRecord(loggerName, target, argsText,
                ignoreResult ? InvocationLogSupport.EMPTY_PAYLOAD
                        : InvocationLogSupport.safeBuildResultText(result, runtimeJsonSerializer, invocationProperties),
                InvocationLogSupport.elapsedMs(start), null);
        InvocationLogSupport.safeWrite(invocationLogWriter, record);
        return result;
    }

    private InvocationLogRecord buildRecord(String loggerName, String target, String argsText, String resultText, long costMs,
            Throwable error) {
        InvocationLogRecord record = new InvocationLogRecord();
        record.setLoggerName(loggerName);
        record.setTraceId(TraceContext.get(properties.getLog().getTrace().getMdcKey()));
        record.setSource(InvocationLogSource.CONTROLLER);
        record.setTarget(target);
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

    private String buildRequestTarget() {
        if (!WebUtils.isWebContext()) {
            return "";
        }

        return WebUtils.getRequestIp() + ' ' + WebUtils.getRequestMethod() + ' ' + resolveRequestPath();
    }

    private String resolveRequestPath() {
        Object pattern = WebUtils.getRequest().getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern != null) {
            return String.valueOf(pattern);
        }
        return WebUtils.getRequestURI();
    }
}
