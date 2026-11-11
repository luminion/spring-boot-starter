package io.github.luminion.velo.log.aspect;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.AnnotatedMethodPointcut;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.trace.TraceScopeManager;
import io.github.luminion.velo.trace.W3cTraceContextResolver;
import org.springframework.aop.Pointcut;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;

/**
 * 任务入口日志适配。
 */
public class ScheduledLogAspect extends SourceLogAspectSupport {
    public ScheduledLogAspect(VeloProperties properties, InvocationLogEngine engine) {
        this(engine, new TraceScopeManager(properties.getLog().getTrace(), new W3cTraceContextResolver()));
    }

    public ScheduledLogAspect(InvocationLogEngine engine, TraceScopeManager trace) {
        super(engine, trace, InvocationLogSource.SCHEDULED);
    }

    /**
     * 任务入口切点：方法上声明了 {@link Scheduled} 或 {@link Schedules}。
     *
     * @return 静态切点
     */
    public static Pointcut pointcut() {
        return AnnotatedMethodPointcut.forMethods(Scheduled.class, Schedules.class);
    }
}
