package io.github.luminion.velo.feign;

import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.log.InvocationLogEngine;
import io.github.luminion.velo.log.InvocationLogSource;
import io.github.luminion.velo.log.InvocationLogSupport;
import io.github.luminion.velo.log.LogInvocation;
import io.github.luminion.velo.log.trace.TraceContext;
import io.github.luminion.velo.log.trace.TraceScopeManager;

import java.lang.reflect.Method;

import lombok.Getter;
import lombok.Setter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.util.ReflectionUtils;

/**
 * Feign 逻辑客户端调用日志切面。
 */
@Aspect
public class FeignLogAspect implements Ordered {
    private final InvocationLogEngine engine;
    private final TraceScopeManager trace;
    @Getter
    @Setter
    private int order = VeloAdvisorOrder.LOG_FEIGN;

    public FeignLogAspect(InvocationLogEngine engine) {
        this(engine, null);
    }

    public FeignLogAspect(InvocationLogEngine engine, TraceScopeManager trace) {
        this.engine = engine;
        this.trace = trace;
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
            String target = FeignLogSupport.buildInvocationTarget(method, metadata);
            FeignInvocationContext context = FeignInvocationContext.open();
            try {
                LogInvocation invocation = InvocationLogSupport.invocation(point, InvocationLogSource.FEIGN, target);
                LogInvocation.LogInvocationBuilder invocationBuilder = invocation.toBuilder();
                LogInvocation logInvocation = invocationBuilder
                        .requestHeaders(context::getRequestHeaders)
                        .responseHeaders(context::getResponseHeaders)
                        .build();
                return engine.invoke(logInvocation, point::proceed);
            } finally {
                FeignInvocationContext.close();
            }
        }
    }
}
