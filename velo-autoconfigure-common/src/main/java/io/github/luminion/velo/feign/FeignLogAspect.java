package io.github.luminion.velo.feign;

import io.github.luminion.velo.core.AnnotatedMethodPointcut;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.model.InvocationLogContext;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.model.LogInvocation;
import io.github.luminion.velo.log.support.InvocationLogSupport;
import io.github.luminion.velo.trace.TraceContext;
import io.github.luminion.velo.trace.TraceScopeManager;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;

/**
 * Feign 逻辑客户端调用日志拦截器。
 */
public class FeignLogAspect implements MethodInterceptor {
    private static final String FEIGN_CLIENT = "org.springframework.cloud.openfeign.FeignClient";

    private final InvocationLogEngine engine;
    private final TraceScopeManager trace;
    private final Environment environment;

    public FeignLogAspect(InvocationLogEngine engine) {
        this(engine, null);
    }

    public FeignLogAspect(InvocationLogEngine engine, TraceScopeManager trace) {
        this(engine, trace, null);
    }

    /**
     * 创建 Feign 调用日志拦截器。
     *
     * @param engine      日志协调器，为 {@code null} 时只维护 trace
     * @param trace       链路作用域，为 {@code null} 时不维护 trace
     * @param environment 用于解析 {@code @FeignClient} 属性中的占位符，可为 {@code null}
     */
    public FeignLogAspect(InvocationLogEngine engine, TraceScopeManager trace, Environment environment) {
        this.engine = engine;
        this.trace = trace;
        this.environment = environment;
    }

    /**
     * Feign 客户端切点：Feign 生成的 JDK 代理（接口或其父接口声明了 {@code @FeignClient}）的 public 方法，
     * 或直接声明了 {@code @FeignClient} 的类型；只实现客户端接口的普通类（如 fallback）不匹配。
     *
     * @return 静态切点
     */
    public static Pointcut pointcut() {
        StaticMethodMatcherPointcut pointcut = new StaticMethodMatcherPointcut() {
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                if (!AnnotatedMethodPointcut.isInterceptable(method, true)) {
                    return false;
                }
                Class<?> type = targetClass == null ? method.getDeclaringClass() : targetClass;
                MergedAnnotations.SearchStrategy strategy = Proxy.isProxyClass(type)
                        ? MergedAnnotations.SearchStrategy.TYPE_HIERARCHY : MergedAnnotations.SearchStrategy.DIRECT;
                return MergedAnnotations.from(type, strategy).isPresent(FEIGN_CLIENT);
            }
        };
        pointcut.setClassFilter(clazz -> !clazz.getName().startsWith("java."));
        return pointcut;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        if (ReflectionUtils.isObjectMethod(method)) {
            return invocation.proceed();
        }
        try (TraceContext.Scope scope = trace == null ? null : trace.open()) {
            if (engine == null) {
                return invocation.proceed();
            }
            FeignRequestMetadata metadata = FeignClientMetadataResolver.resolveRequestMetadata(method);
            String target = method.getName() + "()";
            Object targetObject = invocation.getThis();
            Class<?> type = targetObject == null ? method.getDeclaringClass() : AopUtils.getTargetClass(targetObject);
            String rawClient = FeignClientMetadataResolver.resolveClientAttribute(type, "name");
            String client = resolve(rawClient);
            if (client.isEmpty()) {
                String rawValue = FeignClientMetadataResolver.resolveClientAttribute(type, "value");
                client = resolve(rawValue);
            }
            String rawContextId = FeignClientMetadataResolver.resolveClientAttribute(type, "contextId");
            String contextId = resolve(rawContextId);
            String clientName = client;
            String clientContextId = contextId.isEmpty() ? client : contextId;
            FeignInvocationContext context = FeignInvocationContext.open();
            try {
                LogInvocation logInvocation = InvocationLogSupport.invocation(invocation, InvocationLogSource.FEIGN, target)
                        .toBuilder()
                        .context(() -> InvocationLogContext.builder().clientName(clientName).contextId(clientContextId)
                                .httpMethod(metadata.getHttpMethod()).path(metadata.getPath())
                                .instanceAddress(context.getInstanceAddress()).build())
                        .requestHeaders(context::getRequestHeaders)
                        .responseHeaders(context::getResponseHeaders)
                        .build();
                return engine.invoke(logInvocation, invocation::proceed);
            } finally {
                FeignInvocationContext.close();
            }
        }
    }

    private String resolve(String value) {
        return environment == null ? value : environment.resolvePlaceholders(value);
    }
}
