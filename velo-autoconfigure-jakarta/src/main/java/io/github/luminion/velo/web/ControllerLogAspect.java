package io.github.luminion.velo.web;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogSource;
import io.github.luminion.velo.log.InvocationLogSupport;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.trace.TraceContext;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import io.github.luminion.velo.core.util.WebUtils;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Controller 调用日志切面。
 */
@Aspect
@RequiredArgsConstructor
public class ControllerLogAspect {

    private final VeloProperties properties;

    private final RuntimeJsonSerializer runtimeJsonSerializer;

    private final InvocationLogWriter invocationLogWriter;

    @Around("execution(public * *(..)) && (within(@org.springframework.web.bind.annotation.RestController *) || @annotation(org.springframework.web.bind.annotation.ResponseBody) || @within(org.springframework.web.bind.annotation.ResponseBody))")
    public Object logControllerInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String target = buildRequestTarget();
        VeloProperties.InvocationProperties invocationProperties = properties.getLog().getInvocation();
        String argsText = InvocationLogSupport.buildArgsText(signature, joinPoint.getTarget(), joinPoint.getArgs(),
                runtimeJsonSerializer, invocationProperties);
        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            InvocationLogRecord record = buildRecord(target, argsText, InvocationLogSupport.buildResultText(result,
                    runtimeJsonSerializer, invocationProperties), InvocationLogSupport.elapsedMs(start), null);
            invocationLogWriter.write(record);
            return result;
        } catch (Throwable ex) {
            InvocationLogRecord record = buildRecord(target, argsText, null, InvocationLogSupport.elapsedMs(start), ex);
            invocationLogWriter.write(record);
            throw ex;
        }
    }

    private InvocationLogRecord buildRecord(String target, String argsText, String resultText, long costMs,
            Throwable error) {
        InvocationLogRecord record = new InvocationLogRecord();
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
