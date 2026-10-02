package io.github.luminion.velo.log.aspect;

import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.log.InvocationLogEngine;
import io.github.luminion.velo.log.InvocationLogSource;
import io.github.luminion.velo.log.InvocationLogSupport;
import io.github.luminion.velo.log.trace.TraceContext;
import io.github.luminion.velo.log.trace.TraceScopeManager;
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
    private final TraceScopeManager trace;
    @Getter
    private final int order = VeloAdvisorOrder.LOG_CONTROLLER;

    protected SourceLogAspectSupport(
            InvocationLogEngine engine, TraceScopeManager trace, InvocationLogSource source) {
        this.engine = engine;
        this.source = source;
        this.trace = trace;
    }

    protected Object log(ProceedingJoinPoint point) throws Throwable {
        try (TraceContext.Scope scope = trace == null ? null : trace.root()) {
            if (engine == null) {
                return point.proceed();
            }
            MethodSignature signature = (MethodSignature) point.getSignature();
            String target = signature.getName() + "()";
            return engine.invoke(InvocationLogSupport.invocation(point, source, target), point::proceed);
        }
    }
}
