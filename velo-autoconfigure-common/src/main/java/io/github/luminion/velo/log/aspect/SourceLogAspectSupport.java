package io.github.luminion.velo.log.aspect;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.log.InvocationLogEngine;
import io.github.luminion.velo.log.InvocationLogSource;
import io.github.luminion.velo.log.InvocationLogSupport;
import io.github.luminion.velo.log.trace.TraceContext;
import lombok.Getter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;

/**
 * 每次任务执行建立独立 trace 作用域，完成后恢复工作线程上下文。
 */
public abstract class SourceLogAspectSupport implements Ordered {
    private final InvocationLogEngine engine;
    private final InvocationLogSource source;
    @Getter
    private final int order = VeloAdvisorOrder.LOG_CONTROLLER;

    protected SourceLogAspectSupport(
            VeloProperties properties, InvocationLogEngine engine, InvocationLogSource source) {
        this.engine = engine;
        this.source = source;
    }

    protected Object log(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        String target = signature.getName() + "()";
        try (TraceContext.Scope scope = engine.openRootTrace()) {
            return engine.invoke(InvocationLogSupport.invocation(point, source, target), point::proceed);
        }
    }
}
