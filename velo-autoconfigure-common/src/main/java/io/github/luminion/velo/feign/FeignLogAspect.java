package io.github.luminion.velo.feign;

import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.model.InvocationLogContext;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.model.LogInvocation;
import io.github.luminion.velo.log.support.InvocationLogSupport;
import io.github.luminion.velo.trace.TraceContext;
import io.github.luminion.velo.trace.TraceScopeManager;
import java.lang.reflect.Method;
import lombok.Getter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;

/**
 * Feign 逻辑客户端调用日志切面。
 */
@Aspect
public class FeignLogAspect implements Ordered {
    private final InvocationLogEngine engine;
    private final TraceScopeManager trace;
    private final Environment environment;
    @Getter
    private final int order;

    public FeignLogAspect(InvocationLogEngine engine) {
        this(engine, null);
    }

    public FeignLogAspect(InvocationLogEngine engine, TraceScopeManager trace) {
        this(engine, trace, VeloAdvisorOrder.LOG_FEIGN);
    }

    public FeignLogAspect(InvocationLogEngine engine, TraceScopeManager trace, int order) {
        this(engine, trace, order, null);
    }

    public FeignLogAspect(InvocationLogEngine engine, TraceScopeManager trace, int order, Environment environment) {
        this.engine = engine;
        this.trace = trace;
        this.order = order;
        this.environment = environment;
    }

    @Around("execution(" +
            "public * *(..)) " +
            "&& (within(@org.springframework.cloud.openfeign.FeignClient *) " +
            "|| @within(org.springframework.cloud.openfeign.FeignClient)" +
            ")")
    public Object logFeignInvocation(ProceedingJoinPoint point) throws Throwable {
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        if (ReflectionUtils.isObjectMethod(method)) {
            return point.proceed();
        }
        try (TraceContext.Scope scope = trace == null ? null : trace.open()) {
            if (engine == null) {
                return point.proceed();
            }
            FeignRequestMetadata metadata = FeignClientMetadataResolver.resolveRequestMetadata(method);
            String target = method.getName() + "()";
            Class<?> type = point.getTarget() == null ? method.getDeclaringClass() : AopUtils.getTargetClass(point.getTarget());
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
                LogInvocation invocation = InvocationLogSupport.invocation(point, InvocationLogSource.FEIGN, target);
                LogInvocation.LogInvocationBuilder invocationBuilder = invocation.toBuilder();
                LogInvocation logInvocation = invocationBuilder
                        .context(() -> InvocationLogContext.builder().clientName(clientName).contextId(clientContextId)
                                .httpMethod(metadata.getHttpMethod()).path(metadata.getPath())
                                .instanceAddress(context.getInstanceAddress()).build())
                        .requestHeaders(context::getRequestHeaders)
                        .responseHeaders(context::getResponseHeaders)
                        .build();
                return engine.invoke(logInvocation, point::proceed);
            } finally {
                FeignInvocationContext.close();
            }
        }
    }
    private String resolve(String value) {
        return environment == null ? value : environment.resolvePlaceholders(value);
    }
}
