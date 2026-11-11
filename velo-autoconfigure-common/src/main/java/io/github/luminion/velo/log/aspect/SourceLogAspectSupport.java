package io.github.luminion.velo.log.aspect;

import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.support.InvocationLogSupport;
import io.github.luminion.velo.trace.TraceContext;
import io.github.luminion.velo.trace.TraceScopeManager;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * 每次任务执行建立独立 trace 作用域，完成后恢复工作线程上下文。
 */
public abstract class SourceLogAspectSupport implements MethodInterceptor {
    private final InvocationLogEngine engine;
    private final InvocationLogSource source;
    private final TraceScopeManager trace;

    protected SourceLogAspectSupport(
            InvocationLogEngine engine, TraceScopeManager trace, InvocationLogSource source) {
        this.engine = engine;
        this.source = source;
        this.trace = trace;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        try (TraceContext.Scope scope = trace == null ? null : trace.root()) {
            if (engine == null) {
                return invocation.proceed();
            }
            String target = invocation.getMethod().getName() + "()";
            return engine.invoke(InvocationLogSupport.invocation(invocation, source, target), invocation::proceed);
        }
    }
}
