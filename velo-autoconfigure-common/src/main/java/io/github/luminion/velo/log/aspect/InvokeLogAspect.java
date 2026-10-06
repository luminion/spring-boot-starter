package io.github.luminion.velo.log.aspect;

import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.log.InvocationLogEngine;
import io.github.luminion.velo.log.InvocationLogSource;
import io.github.luminion.velo.log.InvocationLogSupport;
import io.github.luminion.velo.log.trace.TraceContext;
import io.github.luminion.velo.log.trace.TraceScopeManager;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import lombok.Getter;
import lombok.Setter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 普通方法调用日志切面。
 */
@Aspect
public class InvokeLogAspect implements Ordered {
    private final InvocationLogEngine engine;
    private final TraceScopeManager trace;
    @Getter
    @Setter
    private int order = VeloAdvisorOrder.LOG_INVOKE;

    public InvokeLogAspect(InvocationLogEngine engine) {
        this(engine, null);
    }

    public InvokeLogAspect(InvocationLogEngine engine, TraceScopeManager trace) {
        this.engine = engine;
        this.trace = trace;
    }

    @Around(
            "@within(io.github.luminion.velo.log.annotation.InvokeLog) || "
                    + "@annotation(io.github.luminion.velo.log.annotation.InvokeLog)")
    public Object logInvocation(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Class<?> type =
                point.getTarget() == null
                        ? signature.getDeclaringType()
                        : AopUtils.getTargetClass(point.getTarget());
        Method method = AopUtils.getMostSpecificMethod(signature.getMethod(), type);
        if (AnnotatedElementUtils.hasAnnotation(type, RestController.class)
                || AnnotatedElementUtils.hasAnnotation(type, ResponseBody.class)
                || AnnotatedElementUtils.hasAnnotation(method, ResponseBody.class)
                || hasFeignClient(type)
                || hasFeignClient(signature.getDeclaringType())
                || isAutomaticSource(method)) {
            return point.proceed();
        }
        try (TraceContext.Scope scope = trace == null ? null : trace.open()) {
            if (engine == null) {
                return point.proceed();
            }
            return engine.invoke(
                    InvocationLogSupport.invocation(
                            point, InvocationLogSource.INVOKE, signature.getName() + "()"),
                    point::proceed);
        }
    }

    private boolean hasFeignClient(Class<?> type) {
        for (Annotation annotation : type.getAnnotations()) {
            if ("org.springframework.cloud.openfeign.FeignClient"
                    .equals(annotation.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isAutomaticSource(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            String name = annotation.annotationType().getName();
            if (name.equals("org.springframework.scheduling.annotation.Scheduled")
                    || name.equals("org.springframework.scheduling.annotation.Schedules")
                    || name.equals("com.xxl.job.core.handler.annotation.XxlJob")) {
                return true;
            }
        }
        return false;
    }
}
