package io.github.luminion.velo.web;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogSource;
import io.github.luminion.velo.log.InvocationLogSupport;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.InvocationPhase;
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
 *
 * <p>每次调用写两条记录：进入时写 {@link InvocationPhase#ENTRY}（含入参），
 * 退出时写 {@link InvocationPhase#EXIT}（含耗时与返回值或异常）。</p>
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

        // 进入日志：记录请求路径与入参
        InvocationLogSupport.safeWrite(invocationLogWriter, buildEntryRecord(loggerName, target, argsText));

        long start = System.nanoTime();
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            InvocationLogRecord exitRecord = buildExitRecord(loggerName, target,
                    null, InvocationLogSupport.elapsedMs(start), ex);
            InvocationLogSupport.safeWrite(invocationLogWriter, exitRecord);
            throw ex;
        }

        InvocationLogRecord exitRecord = buildExitRecord(loggerName, target,
                ignoreResult ? InvocationLogSupport.EMPTY_PAYLOAD
                        : InvocationLogSupport.safeBuildResultText(result, runtimeJsonSerializer, invocationProperties),
                InvocationLogSupport.elapsedMs(start), null);
        InvocationLogSupport.safeWrite(invocationLogWriter, exitRecord);
        return result;
    }

    private InvocationLogRecord buildEntryRecord(String loggerName, String target, String argsText) {
        InvocationLogRecord record = new InvocationLogRecord();
        record.setLoggerName(loggerName);
        record.setTraceId(TraceContext.get(properties.getLog().getTrace().getMdcKey()));
        record.setSource(InvocationLogSource.CONTROLLER);
        record.setTarget(target);
        record.setPhase(InvocationPhase.ENTRY);
        record.setArgs(argsText);
        record.setSuccess(true);
        return record;
    }

    private InvocationLogRecord buildExitRecord(String loggerName, String target,
            String resultText, long costMs, Throwable error) {
        InvocationLogRecord record = new InvocationLogRecord();
        record.setLoggerName(loggerName);
        record.setTraceId(TraceContext.get(properties.getLog().getTrace().getMdcKey()));
        record.setSource(InvocationLogSource.CONTROLLER);
        record.setTarget(target);
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
