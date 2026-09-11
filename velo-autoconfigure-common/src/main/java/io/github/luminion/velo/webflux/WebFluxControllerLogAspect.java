package io.github.luminion.velo.webflux;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogSource;
import io.github.luminion.velo.log.InvocationLogSupport;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.InvocationPhase;
import io.github.luminion.velo.log.annotation.LogPayloadIgnore;
import io.github.luminion.velo.log.trace.TraceContext;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Publisher;

/**
 * WebFlux Controller 调用日志切面。
 *
 * <p>Mono 在完成、异常或取消时记录一次退出日志；Flux 不缓存或消费完整响应体，
 * 仅在完成时记录元素数量，在异常或取消时记录对应状态。这样不会因为日志功能把流式
 * 响应转换成阻塞或全量缓冲流程。</p>
 *
 * @author luminion
 * @since 1.3.1
 */
@Aspect
public class WebFluxControllerLogAspect implements Ordered {

    private final VeloProperties properties;

    private final RuntimeJsonSerializer runtimeJsonSerializer;

    private final InvocationLogWriter invocationLogWriter;

    private int order = VeloAdvisorOrder.LOG_CONTROLLER;

    public WebFluxControllerLogAspect(VeloProperties properties, RuntimeJsonSerializer runtimeJsonSerializer,
            InvocationLogWriter invocationLogWriter) {
        this.properties = properties;
        this.runtimeJsonSerializer = runtimeJsonSerializer;
        this.invocationLogWriter = invocationLogWriter;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Around("execution(public * *(..)) && (within(@org.springframework.web.bind.annotation.RestController *) || @annotation(org.springframework.web.bind.annotation.ResponseBody) || @within(org.springframework.web.bind.annotation.ResponseBody))")
    public Object logControllerInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> declaringType = signature.getDeclaringType();
        String loggerName = declaringType == null ? null : declaringType.getName();
        VeloProperties.InvocationProperties invocationProperties = properties.getLog().getInvocation();
        LogPayloadIgnore payloadIgnore = InvocationLogSupport.findLogPayloadIgnore(signature, joinPoint.getTarget());
        boolean ignoreArgs = payloadIgnore != null && payloadIgnore.args();
        boolean ignoreResult = payloadIgnore != null && payloadIgnore.result();
        String argsText = ignoreArgs ? InvocationLogSupport.IGNORED_PAYLOAD
                : InvocationLogSupport.safeBuildArgsText(signature, joinPoint.getTarget(), joinPoint.getArgs(),
                        runtimeJsonSerializer, invocationProperties);
        boolean declaredReactive = isPublisherType(signature.getReturnType());
        boolean entryWritten = false;
        if (!declaredReactive) {
            writeEntry(loggerName, buildRequestTarget(findExchange(joinPoint.getArgs())),
                    currentTraceId(), argsText);
            entryWritten = true;
        }

        long start = System.nanoTime();
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            if (!entryWritten) {
                writeEntry(loggerName, buildRequestTarget(findExchange(joinPoint.getArgs())),
                        currentTraceId(), argsText);
            }
            writeExit(loggerName, buildRequestTarget(findExchange(joinPoint.getArgs())), currentTraceId(),
                    null, InvocationLogSupport.elapsedMs(start), ex);
            throw ex;
        }

        if (result instanceof Mono<?>) {
            return logMono((Mono<?>) result, signature, loggerName, argsText, ignoreResult, entryWritten);
        }
        if (result instanceof Flux<?>) {
            return logFlux((Flux<?>) result, loggerName, argsText, ignoreResult, entryWritten);
        }
        if (result instanceof Publisher<?>) {
            return logFlux(Flux.from((Publisher<?>) result), loggerName, argsText, ignoreResult, entryWritten);
        }

        if (!entryWritten) {
            writeEntry(loggerName, buildRequestTarget(findExchange(joinPoint.getArgs())),
                    currentTraceId(), argsText);
        }
        String resultText = isVoidReturn(signature) ? InvocationLogSupport.VOID_RESULT
                : ignoreResult ? InvocationLogSupport.IGNORED_PAYLOAD
                        : InvocationLogSupport.safeBuildResultText(result, runtimeJsonSerializer,
                                invocationProperties);
        writeExit(loggerName, buildRequestTarget(findExchange(joinPoint.getArgs())), currentTraceId(),
                resultText, InvocationLogSupport.elapsedMs(start), null);
        return result;
    }

    private Mono<?> logMono(Mono<?> mono, MethodSignature signature, String loggerName, String argsText,
            boolean ignoreResult, boolean entryWritten) {
        VeloProperties.InvocationProperties invocationProperties = properties.getLog().getInvocation();
        return Mono.deferContextual(contextView -> {
            ServerWebExchange exchange = WebFluxContext.exchange(contextView);
            String target = entryWritten ? buildRequestTarget(null) : buildRequestTarget(exchange);
            String traceId = resolveTraceId(contextView);
            if (!entryWritten) {
                writeEntry(loggerName, target, traceId, argsText);
            }
            long start = System.nanoTime();
            AtomicBoolean logged = new AtomicBoolean();
            return mono.doOnSuccess(value -> {
                if (logged.compareAndSet(false, true)) {
                    String resultText = isVoidReturn(signature) ? InvocationLogSupport.VOID_RESULT
                            : ignoreResult ? InvocationLogSupport.IGNORED_PAYLOAD
                                    : InvocationLogSupport.safeBuildResultText(value, runtimeJsonSerializer,
                                            invocationProperties);
                    writeExit(loggerName, target, traceId, resultText,
                            InvocationLogSupport.elapsedMs(start), null);
                }
            }).doOnError(error -> {
                if (logged.compareAndSet(false, true)) {
                    writeExit(loggerName, target, traceId, null, InvocationLogSupport.elapsedMs(start), error);
                }
            }).doOnCancel(() -> {
                if (logged.compareAndSet(false, true)) {
                    writeExit(loggerName, target, traceId, null, InvocationLogSupport.elapsedMs(start),
                            new CancellationException("WebFlux Mono 订阅已取消"));
                }
            });
        });
    }

    private Flux<?> logFlux(Flux<?> flux, String loggerName, String argsText, boolean ignoreResult,
            boolean entryWritten) {
        return Flux.deferContextual(contextView -> {
            ServerWebExchange exchange = WebFluxContext.exchange(contextView);
            String target = entryWritten ? buildRequestTarget(null) : buildRequestTarget(exchange);
            String traceId = resolveTraceId(contextView);
            if (!entryWritten) {
                writeEntry(loggerName, target, traceId, argsText);
            }
            long start = System.nanoTime();
            AtomicLong count = new AtomicLong();
            AtomicBoolean logged = new AtomicBoolean();
            return flux.doOnNext(value -> count.incrementAndGet())
                    .doOnComplete(() -> {
                        if (logged.compareAndSet(false, true)) {
                            String resultText = ignoreResult ? InvocationLogSupport.IGNORED_PAYLOAD
                                    : buildFluxResultText(count.get());
                            writeExit(loggerName, target, traceId, resultText,
                                    InvocationLogSupport.elapsedMs(start), null);
                        }
                    })
                    .doOnError(error -> {
                        if (logged.compareAndSet(false, true)) {
                            writeExit(loggerName, target, traceId, null,
                                    InvocationLogSupport.elapsedMs(start), error);
                        }
                    })
                    .doOnCancel(() -> {
                        if (logged.compareAndSet(false, true)) {
                            writeExit(loggerName, target, traceId, null,
                                    InvocationLogSupport.elapsedMs(start),
                                    new CancellationException("WebFlux Flux 订阅已取消"));
                        }
                    });
        });
    }

    private String buildFluxResultText(long count) {
        VeloProperties.InvocationProperties invocationProperties = properties.getLog().getInvocation();
        if (!invocationProperties.isIncludeResult() || invocationProperties.getMaxPayloadLength() == 0) {
            return InvocationLogSupport.DISABLED_PAYLOAD;
        }
        String text = "Flux{count=" + count + "}";
        int maxLength = invocationProperties.getMaxPayloadLength();
        if (maxLength < 0 || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private void writeEntry(String loggerName, String target, String traceId, String argsText) {
        InvocationLogRecord record = new InvocationLogRecord();
        record.setLoggerName(loggerName);
        record.setTraceId(traceId);
        record.setSource(InvocationLogSource.CONTROLLER);
        record.setTarget(target);
        record.setPhase(InvocationPhase.ENTRY);
        record.setArgs(argsText);
        record.setSuccess(true);
        InvocationLogSupport.safeWrite(invocationLogWriter, record);
    }

    private void writeExit(String loggerName, String target, String traceId, String resultText, long costMs,
            Throwable error) {
        InvocationLogRecord record = new InvocationLogRecord();
        record.setLoggerName(loggerName);
        record.setTraceId(traceId);
        record.setSource(InvocationLogSource.CONTROLLER);
        record.setTarget(target);
        record.setPhase(InvocationPhase.EXIT);
        record.setCostMs(costMs);
        record.setSuccess(error == null);
        if (error == null) {
            record.setResult(resultText);
        } else {
            record.setError(error);
            record.setErrorClass(error.getClass().getName());
            record.setErrorMessage(error.getMessage());
        }
        InvocationLogSupport.safeWrite(invocationLogWriter, record);
    }

    private String buildRequestTarget(ServerWebExchange exchange) {
        if (exchange == null) {
            return "";
        }
        return WebFluxUtils.getRequestIp(exchange) + ' ' + WebFluxUtils.getRequestMethod(exchange) + ' '
                + WebFluxUtils.getRequestPath(exchange);
    }

    private ServerWebExchange findExchange(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof ServerWebExchange) {
                return (ServerWebExchange) arg;
            }
        }
        return null;
    }

    private String resolveTraceId(reactor.util.context.ContextView contextView) {
        String traceId = WebFluxContext.traceId(contextView);
        if (traceId != null) {
            return traceId;
        }
        return currentTraceId();
    }

    private String currentTraceId() {
        return TraceContext.get(properties.getLog().getTrace().getMdcKey());
    }

    private boolean isPublisherType(Class<?> returnType) {
        return returnType != null && Publisher.class.isAssignableFrom(returnType);
    }

    private boolean isVoidReturn(MethodSignature signature) {
        return signature.getReturnType() == Void.TYPE;
    }
}
