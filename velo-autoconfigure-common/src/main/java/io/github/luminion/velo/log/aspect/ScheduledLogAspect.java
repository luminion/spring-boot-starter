package io.github.luminion.velo.log.aspect;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogEngine;
import io.github.luminion.velo.log.InvocationLogSource;
import io.github.luminion.velo.log.trace.TraceScopeManager;
import io.github.luminion.velo.log.trace.W3cTraceContextResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * 任务入口日志适配。
 */
@Aspect
public class ScheduledLogAspect extends SourceLogAspectSupport {
    public ScheduledLogAspect(VeloProperties properties, InvocationLogEngine engine) {
        this(engine, new TraceScopeManager(properties.getLog().getTrace(), new W3cTraceContextResolver()));
    }

    public ScheduledLogAspect(InvocationLogEngine engine, TraceScopeManager trace) {
        super(engine, trace, InvocationLogSource.SCHEDULED);
    }

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled) || "
            + "@annotation(org.springframework.scheduling.annotation.Schedules)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        return log(point);
    }
}
