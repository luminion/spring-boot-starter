package io.github.luminion.velo.webflux;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.ReactiveTypeSupport;
import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.log.InvocationLogSupport;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.annotation.LogPayloadIgnore;
import io.github.luminion.velo.log.annotation.SlowLog;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebFlux 响应式慢调用日志切面。
 *
 * <p>耗时从订阅开始计算，到响应式链完成、异常或取消时判断，避免把延迟执行的
 * Publisher 误判为“瞬时完成”。</p>
 *
 * @author luminion
 * @since 1.3.1
 */
@Aspect
public class WebFluxSlowLogAspect implements Ordered {

    private final VeloProperties properties;
    private final ObjectProvider<RuntimeJsonSerializer> runtimeJsonSerializerProvider;
    private final InvocationLogWriter invocationLogWriter;

    private int order = VeloAdvisorOrder.LOG_SLOW;

    public WebFluxSlowLogAspect(VeloProperties properties,
            ObjectProvider<RuntimeJsonSerializer> runtimeJsonSerializerProvider,
            InvocationLogWriter invocationLogWriter) {
        this.properties = properties;
        this.runtimeJsonSerializerProvider = runtimeJsonSerializerProvider;
        this.invocationLogWriter = invocationLogWriter;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Around("@within(io.github.luminion.velo.log.annotation.SlowLog) " +
            "|| @annotation(io.github.luminion.velo.log.annotation.SlowLog)")
    public Object logTime(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        if (!ReactiveTypeSupport.isReactiveType(signature.getReturnType())) {
            return joinPoint.proceed();
        }

        SlowLog slowLog = resolveSlowLog(signature, joinPoint.getTarget());
        if (slowLog == null) {
            return joinPoint.proceed();
        }
        if (slowLog.value() <= 0L) {
            throw new IllegalArgumentException("Slow log threshold must be greater than zero.");
        }

        LogPayloadIgnore payloadIgnore = InvocationLogSupport.findLogPayloadIgnore(signature, joinPoint.getTarget());
        boolean ignoreArgs = payloadIgnore != null && payloadIgnore.args();
        boolean ignoreResult = payloadIgnore != null && payloadIgnore.result();
        RuntimeJsonSerializer serializer = WebFluxInvocationLogSupport.resolveSerializer(
                runtimeJsonSerializerProvider);
        VeloProperties.InvocationProperties invocationProperties = properties.getLog().getInvocation();
        String loggerName = WebFluxInvocationLogSupport.loggerName(signature);
        String target = signature.getName() + "()";
        if (WebFluxReactiveSupport.isMonoReturnType(signature.getReturnType())) {
            return logMono(joinPoint, signature, slowLog, serializer, invocationProperties, loggerName, target,
                    ignoreArgs, ignoreResult);
        }
        return logFlux(joinPoint, signature, slowLog, serializer, invocationProperties, loggerName, target,
                ignoreArgs, ignoreResult);
    }

    private Mono<?> logMono(ProceedingJoinPoint joinPoint, MethodSignature signature, SlowLog slowLog,
            RuntimeJsonSerializer serializer, VeloProperties.InvocationProperties invocationProperties,
            String loggerName, String target, boolean ignoreArgs, boolean ignoreResult) {
        return Mono.deferContextual(contextView -> {
            String traceId = WebFluxInvocationLogSupport.traceId(contextView, properties);
            String argsText = WebFluxInvocationLogSupport.argsText(signature, joinPoint.getTarget(), joinPoint.getArgs(),
                    serializer, invocationProperties, ignoreArgs);
            long start = System.nanoTime();
            AtomicBoolean written = new AtomicBoolean();
            return WebFluxReactiveSupport.proceedMono(joinPoint)
                    .doOnSuccess(value -> {
                        if (written.compareAndSet(false, true)) {
                            long elapsedNanos = InvocationLogSupport.elapsedNanos(start);
                            if (InvocationLogSupport.exceedsSlowThresholdNanos(elapsedNanos, slowLog.value())) {
                                WebFluxInvocationLogSupport.writeSlow(invocationLogWriter, loggerName, target, traceId,
                                        WebFluxInvocationLogSupport.resultText(signature, value, serializer,
                                                invocationProperties, ignoreResult),
                                        InvocationLogSupport.nanosToMillis(elapsedNanos), null, argsText,
                                        slowLog.value());
                            }
                        }
                    })
                    .doOnError(error -> {
                        if (written.compareAndSet(false, true)) {
                            long elapsedNanos = InvocationLogSupport.elapsedNanos(start);
                            if (InvocationLogSupport.exceedsSlowThresholdNanos(elapsedNanos, slowLog.value())) {
                                WebFluxInvocationLogSupport.writeSlow(invocationLogWriter, loggerName, target, traceId,
                                        null, InvocationLogSupport.nanosToMillis(elapsedNanos), error, argsText,
                                        slowLog.value());
                            }
                        }
                    })
                    .doOnCancel(() -> {
                        if (written.compareAndSet(false, true)) {
                            long elapsedNanos = InvocationLogSupport.elapsedNanos(start);
                            if (InvocationLogSupport.exceedsSlowThresholdNanos(elapsedNanos, slowLog.value())) {
                                WebFluxInvocationLogSupport.writeSlow(invocationLogWriter, loggerName, target, traceId,
                                        null, InvocationLogSupport.nanosToMillis(elapsedNanos),
                                        new CancellationException("WebFlux Mono 订阅已取消"), argsText, slowLog.value());
                            }
                        }
                    });
        });
    }

    private Flux<?> logFlux(ProceedingJoinPoint joinPoint, MethodSignature signature, SlowLog slowLog,
            RuntimeJsonSerializer serializer, VeloProperties.InvocationProperties invocationProperties,
            String loggerName, String target, boolean ignoreArgs, boolean ignoreResult) {
        return Flux.deferContextual(contextView -> {
            String traceId = WebFluxInvocationLogSupport.traceId(contextView, properties);
            String argsText = WebFluxInvocationLogSupport.argsText(signature, joinPoint.getTarget(), joinPoint.getArgs(),
                    serializer, invocationProperties, ignoreArgs);
            long start = System.nanoTime();
            AtomicLong count = new AtomicLong();
            AtomicBoolean written = new AtomicBoolean();
            return WebFluxReactiveSupport.proceedFlux(joinPoint)
                    .doOnNext(value -> count.incrementAndGet())
                    .doOnComplete(() -> {
                        if (written.compareAndSet(false, true)) {
                            long elapsedNanos = InvocationLogSupport.elapsedNanos(start);
                            if (InvocationLogSupport.exceedsSlowThresholdNanos(elapsedNanos, slowLog.value())) {
                                WebFluxInvocationLogSupport.writeSlow(invocationLogWriter, loggerName, target, traceId,
                                        WebFluxInvocationLogSupport.fluxResultText(invocationProperties, ignoreResult,
                                                count.get()),
                                        InvocationLogSupport.nanosToMillis(elapsedNanos), null, argsText,
                                        slowLog.value());
                            }
                        }
                    })
                    .doOnError(error -> {
                        if (written.compareAndSet(false, true)) {
                            long elapsedNanos = InvocationLogSupport.elapsedNanos(start);
                            if (InvocationLogSupport.exceedsSlowThresholdNanos(elapsedNanos, slowLog.value())) {
                                WebFluxInvocationLogSupport.writeSlow(invocationLogWriter, loggerName, target, traceId,
                                        null, InvocationLogSupport.nanosToMillis(elapsedNanos), error, argsText,
                                        slowLog.value());
                            }
                        }
                    })
                    .doOnCancel(() -> {
                        if (written.compareAndSet(false, true)) {
                            long elapsedNanos = InvocationLogSupport.elapsedNanos(start);
                            if (InvocationLogSupport.exceedsSlowThresholdNanos(elapsedNanos, slowLog.value())) {
                                WebFluxInvocationLogSupport.writeSlow(invocationLogWriter, loggerName, target, traceId,
                                        null, InvocationLogSupport.nanosToMillis(elapsedNanos),
                                        new CancellationException("WebFlux Flux 订阅已取消"), argsText, slowLog.value());
                            }
                        }
                    });
        });
    }

    private SlowLog resolveSlowLog(MethodSignature signature, Object target) {
        Class<?> targetType = target != null ? AopUtils.getTargetClass(target) : signature.getDeclaringType();
        Method targetMethod = targetType == null ? signature.getMethod()
                : AopUtils.getMostSpecificMethod(signature.getMethod(), targetType);
        SlowLog slowLog = targetMethod == null ? null
                : AnnotatedElementUtils.findMergedAnnotation(targetMethod, SlowLog.class);
        if (slowLog == null && targetType != null) {
            slowLog = AnnotatedElementUtils.findMergedAnnotation(targetType, SlowLog.class);
        }
        return slowLog;
    }
}
