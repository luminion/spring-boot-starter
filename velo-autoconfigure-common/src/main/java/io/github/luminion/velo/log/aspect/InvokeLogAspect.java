package io.github.luminion.velo.log.aspect;

import io.github.luminion.velo.core.AnnotatedMethodPointcut;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.core.InvocationLogMetadata;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.model.LogInvocation;
import io.github.luminion.velo.log.support.InvocationLogSupport;
import io.github.luminion.velo.trace.TraceContext;
import io.github.luminion.velo.trace.TraceScopeManager;
import java.lang.reflect.Method;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.StaticMethodMatcherPointcut;

/**
 * 普通方法调用日志拦截器；切点只做轻量静态匹配，完整策略在方法首次调用时解析并缓存。
 */
public class InvokeLogAspect implements MethodInterceptor {
    private final InvocationLogEngine engine;
    private final TraceScopeManager trace;

    public InvokeLogAspect(InvocationLogEngine engine) {
        this(engine, null);
    }

    /**
     * 创建普通方法调用日志拦截器。
     *
     * @param engine 日志协调器，为 {@code null} 时只维护 trace
     * @param trace  链路作用域，为 {@code null} 时不维护 trace
     */
    public InvokeLogAspect(InvocationLogEngine engine, TraceScopeManager trace) {
        this.engine = engine;
        this.trace = trace;
    }

    /**
     * 普通方法入口切点：方法或类型上声明了日志功能注解（含组合注解），且不属于 Controller、Feign 或任务等自动入口。
     *
     * @return 静态切点
     */
    public static Pointcut pointcut() {
        StaticMethodMatcherPointcut pointcut = new StaticMethodMatcherPointcut() {
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                return AnnotatedMethodPointcut.isInterceptable(method, false)
                        && InvocationLogMetadata.isOrdinaryEntry(method, targetClass);
            }
        };
        pointcut.setClassFilter(clazz -> !clazz.getName().startsWith("java."));
        return pointcut;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        try (TraceContext.Scope scope = trace == null ? null : trace.open()) {
            if (engine == null) {
                return invocation.proceed();
            }
            String target = invocation.getMethod().getName() + "()";
            LogInvocation logInvocation = InvocationLogSupport.invocation(invocation, InvocationLogSource.ANNOTATION, target);
            return engine.invoke(logInvocation, invocation::proceed);
        }
    }
}
