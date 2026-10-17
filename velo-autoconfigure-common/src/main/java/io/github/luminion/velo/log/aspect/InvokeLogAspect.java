package io.github.luminion.velo.log.aspect;

import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.core.InvocationLogMetadata;
import io.github.luminion.velo.log.core.InvocationLogSource;
import io.github.luminion.velo.log.core.LogInvocation;
import io.github.luminion.velo.trace.TraceContext;
import io.github.luminion.velo.trace.TraceScopeManager;

import java.lang.reflect.Method;

import lombok.Getter;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.core.Ordered;

/**
 * 普通方法调用日志入口；静态匹配与策略解析共用缓存，不在每次调用时扫描注解。
 */
public class InvokeLogAspect implements PointcutAdvisor, Ordered {
    private final InvocationLogEngine engine;
    private final TraceScopeManager trace;
    @Getter
    private final int order;
    @Getter
    private final Pointcut pointcut;
    @Getter
    private final Advice advice;

    public InvokeLogAspect(InvocationLogEngine engine) {
        this(engine, null);
    }

    public InvokeLogAspect(InvocationLogEngine engine, TraceScopeManager trace) {
        this(engine, trace, VeloAdvisorOrder.LOG_INVOKE);
    }

    public InvokeLogAspect(InvocationLogEngine engine, TraceScopeManager trace, int order) {
        this.engine = engine;
        this.trace = trace;
        this.order = order;
        this.pointcut = new StaticMethodMatcherPointcut() {
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                return InvocationLogMetadata.isOrdinaryEntry(method, targetClass);
            }
        };
        this.advice = (MethodInterceptor) this::logInvocation;
    }

    public Object logInvocation(MethodInvocation point) throws Throwable {
        Method method = point.getMethod();
        Object target = point.getThis();
        Class<?> type = target == null ? method.getDeclaringClass() : AopUtils.getTargetClass(target);
        try (TraceContext.Scope scope = trace == null ? null : trace.open()) {
            if (engine == null) {
                return point.proceed();
            }
            LogInvocation invocation = LogInvocation.builder()
                    .method(method)
                    .targetClass(type)
                    .source(InvocationLogSource.ANNOTATION)
                    .target(method.getName() + "()")
                    .arguments(point.getArguments())
                    .build();
            return engine.invoke(invocation, point::proceed);
        }
    }

    @Override
    public boolean isPerInstance() {
        return true;
    }
}
