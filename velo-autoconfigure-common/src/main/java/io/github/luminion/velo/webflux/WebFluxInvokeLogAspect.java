package io.github.luminion.velo.webflux;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.ReactiveTypeSupport;
import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.log.InvocationLogSupport;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.annotation.InvokeLog;
import io.github.luminion.velo.log.annotation.LogPayloadIgnore;
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
 * WebFlux 响应式方法调用日志切面。
 *
 * <p>进入日志和退出日志都绑定到订阅生命周期，避免只在响应式方法组装阶段记录一条
 * “已经完成”的假日志；同一个响应式返回值被多次订阅时，每次订阅都会独立记录。</p>
 *
 * @author luminion
 * @since 1.3.1
 */
@Aspect
public class WebFluxInvokeLogAspect implements Ordered {

    private final VeloProperties properties;
    private final ObjectProvider<RuntimeJsonSerializer> runtimeJsonSerializerProvider;
    private final InvocationLogWriter invocationLogWriter;

    private int order = VeloAdvisorOrder.LOG_INVOKE;

    public WebFluxInvokeLogAspect(VeloProperties properties,
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

    @Around("@within(io.github.luminion.velo.log.annotation.InvokeLog) " +
            "|| @annotation(io.github.luminion.velo.log.annotation.InvokeLog)")
    public Object logInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        if (!ReactiveTypeSupport.isReactiveType(signature.getReturnType())) {
            return joinPoint.proceed();
        }

        InvokeLog invokeLog = resolveInvokeLog(signature, joinPoint.getTarget());
        if (invokeLog == null) {
            return joinPoint.proceed();
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
            return logMono(joinPoint, signature, invokeLog, serializer, invocationProperties, loggerName, target,
                    ignoreArgs, ignoreResult);
        }
        return logFlux(joinPoint, signature, invokeLog, serializer, invocationProperties, loggerName, target,
                ignoreArgs, ignoreResult);
    }

    private Mono<?> logMono(ProceedingJoinPoint joinPoint, MethodSignature signature, InvokeLog invokeLog,
            RuntimeJsonSerializer serializer, VeloProperties.InvocationProperties invocationProperties,
            String loggerName, String target, boolean ignoreArgs, boolean ignoreResult) {
        return Mono.deferContextual(contextView -> {
            String traceId = WebFluxInvocationLogSupport.traceId(contextView, properties);
            String argsText = WebFluxInvocationLogSupport.argsText(signature, joinPoint.getTarget(), joinPoint.getArgs(),
                    serializer, invocationProperties, ignoreArgs);
            WebFluxInvocationLogSupport.writeEntry(invocationLogWriter, loggerName, target, traceId, argsText);
            long start = System.nanoTime();
            AtomicBoolean written = new AtomicBoolean();
            return WebFluxReactiveSupport.proceedMono(joinPoint)
                    .doOnSuccess(value -> {
                        if (written.compareAndSet(false, true)) {
                            String finishArgs = invokeLog.argsOnFinish()
                                    ? WebFluxInvocationLogSupport.argsText(signature, joinPoint.getTarget(),
                                            joinPoint.getArgs(), serializer, invocationProperties, ignoreArgs)
                                    : null;
                            String resultText = WebFluxInvocationLogSupport.resultText(signature, value, serializer,
                                    invocationProperties, ignoreResult);
                            WebFluxInvocationLogSupport.writeExit(invocationLogWriter, loggerName, target, traceId,
                                    resultText, InvocationLogSupport.elapsedMs(start), null, finishArgs);
                        }
                    })
                    .doOnError(error -> {
                        if (written.compareAndSet(false, true)) {
                            String finishArgs = invokeLog.argsOnFinish()
                                    ? WebFluxInvocationLogSupport.argsText(signature, joinPoint.getTarget(),
                                            joinPoint.getArgs(), serializer, invocationProperties, ignoreArgs)
                                    : null;
                            WebFluxInvocationLogSupport.writeExit(invocationLogWriter, loggerName, target, traceId,
                                    null, InvocationLogSupport.elapsedMs(start), error, finishArgs);
                        }
                    })
                    .doOnCancel(() -> {
                        if (written.compareAndSet(false, true)) {
                            String finishArgs = invokeLog.argsOnFinish()
                                    ? WebFluxInvocationLogSupport.argsText(signature, joinPoint.getTarget(),
                                            joinPoint.getArgs(), serializer, invocationProperties, ignoreArgs)
                                    : null;
                            WebFluxInvocationLogSupport.writeExit(invocationLogWriter, loggerName, target, traceId,
                                    null, InvocationLogSupport.elapsedMs(start),
                                    new CancellationException("WebFlux Mono 订阅已取消"), finishArgs);
                        }
                    });
        });
    }

    private Flux<?> logFlux(ProceedingJoinPoint joinPoint, MethodSignature signature, InvokeLog invokeLog,
            RuntimeJsonSerializer serializer, VeloProperties.InvocationProperties invocationProperties,
            String loggerName, String target, boolean ignoreArgs, boolean ignoreResult) {
        return Flux.deferContextual(contextView -> {
            String traceId = WebFluxInvocationLogSupport.traceId(contextView, properties);
            String argsText = WebFluxInvocationLogSupport.argsText(signature, joinPoint.getTarget(), joinPoint.getArgs(),
                    serializer, invocationProperties, ignoreArgs);
            WebFluxInvocationLogSupport.writeEntry(invocationLogWriter, loggerName, target, traceId, argsText);
            long start = System.nanoTime();
            AtomicLong count = new AtomicLong();
            AtomicBoolean written = new AtomicBoolean();
            return WebFluxReactiveSupport.proceedFlux(joinPoint)
                    .doOnNext(value -> count.incrementAndGet())
                    .doOnComplete(() -> {
                        if (written.compareAndSet(false, true)) {
                            String finishArgs = invokeLog.argsOnFinish()
                                    ? WebFluxInvocationLogSupport.argsText(signature, joinPoint.getTarget(),
                                            joinPoint.getArgs(), serializer, invocationProperties, ignoreArgs)
                                    : null;
                            String resultText = WebFluxInvocationLogSupport.fluxResultText(invocationProperties,
                                    ignoreResult, count.get());
                            WebFluxInvocationLogSupport.writeExit(invocationLogWriter, loggerName, target, traceId,
                                    resultText, InvocationLogSupport.elapsedMs(start), null, finishArgs);
                        }
                    })
                    .doOnError(error -> {
                        if (written.compareAndSet(false, true)) {
                            String finishArgs = invokeLog.argsOnFinish()
                                    ? WebFluxInvocationLogSupport.argsText(signature, joinPoint.getTarget(),
                                            joinPoint.getArgs(), serializer, invocationProperties, ignoreArgs)
                                    : null;
                            WebFluxInvocationLogSupport.writeExit(invocationLogWriter, loggerName, target, traceId,
                                    null, InvocationLogSupport.elapsedMs(start), error, finishArgs);
                        }
                    })
                    .doOnCancel(() -> {
                        if (written.compareAndSet(false, true)) {
                            String finishArgs = invokeLog.argsOnFinish()
                                    ? WebFluxInvocationLogSupport.argsText(signature, joinPoint.getTarget(),
                                            joinPoint.getArgs(), serializer, invocationProperties, ignoreArgs)
                                    : null;
                            WebFluxInvocationLogSupport.writeExit(invocationLogWriter, loggerName, target, traceId,
                                    null, InvocationLogSupport.elapsedMs(start),
                                    new CancellationException("WebFlux Flux 订阅已取消"), finishArgs);
                        }
                    });
        });
    }

    private InvokeLog resolveInvokeLog(MethodSignature signature, Object target) {
        Class<?> targetType = target != null ? AopUtils.getTargetClass(target) : signature.getDeclaringType();
        Method targetMethod = targetType == null ? signature.getMethod()
                : AopUtils.getMostSpecificMethod(signature.getMethod(), targetType);
        InvokeLog invokeLog = targetMethod == null ? null
                : AnnotatedElementUtils.findMergedAnnotation(targetMethod, InvokeLog.class);
        if (invokeLog == null && targetType != null) {
            invokeLog = AnnotatedElementUtils.findMergedAnnotation(targetType, InvokeLog.class);
        }
        return invokeLog;
    }
}
