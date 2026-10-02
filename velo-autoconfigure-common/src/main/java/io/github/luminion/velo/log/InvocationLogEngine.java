package io.github.luminion.velo.log;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogPolicyResolver.FeaturePolicy;
import io.github.luminion.velo.log.InvocationLogPolicyResolver.Selection;
import io.github.luminion.velo.log.trace.TraceContext;
import io.github.luminion.velo.log.trace.TraceContextResolver;
import io.github.luminion.velo.log.trace.TraceData;
import io.github.luminion.velo.log.trace.W3cTraceContextResolver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 共用的单次调用生命周期；策略、对象转换和输出相互隔离。
 */
public class InvocationLogEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(InvocationLogEngine.class);
    private final VeloProperties properties;
    private final LogValueFormatter formatter;
    private final InvocationLogWriter writer;
    private final InvocationLogPolicyResolver resolver;
    private final TraceContextResolver traceResolver;

    public InvocationLogEngine(
            VeloProperties properties, LogValueFormatter formatter, InvocationLogWriter writer) {
        this(properties, formatter, writer, new W3cTraceContextResolver());
    }

    public InvocationLogEngine(
            VeloProperties properties,
            LogValueFormatter formatter,
            InvocationLogWriter writer,
            TraceContextResolver traceResolver) {
        this.properties = properties;
        this.formatter = formatter;
        this.writer = writer;
        this.resolver = new InvocationLogPolicyResolver(properties);
        this.traceResolver = traceResolver;
    }

    /**
     * 任务入口使用相同解析策略，但与调用方链路隔离。
     */
    public TraceContext.Scope openRootTrace() {
        VeloProperties.TraceProperties trace = properties.getLog().getTrace();
        return TraceContext.root(trace.getMdcKey(), trace.isEnabled(), traceResolver);
    }

    public Object invoke(LogInvocation invocation, InvocationExecution execution) throws Throwable {
        VeloProperties.TraceProperties trace = properties.getLog().getTrace();
        try (TraceContext.Scope scope =
                     TraceContext.open(trace.getMdcKey(), trace.isEnabled(), traceResolver)) {
            Session session = session(invocation);
            if (session != null) {
                session.entry();
            }
            Object result;
            try {
                result = execution.proceed();
            } catch (Throwable error) {
                if (session != null) {
                    session.finish(null, error);
                }
                throw error;
            }
            if (session != null) {
                if (result instanceof CompletionStage<?>) {
                    try {
                        // 仅观察原对象，不返回派生 Future，不改变取消行为。
                        ((CompletionStage<?>) result).whenComplete(session::finish);
                    } catch (RuntimeException error) {
                        LOGGER.warn("Cannot observe invocation completion for {}", invocation.getTarget());
                    }
                } else {
                    session.finish(result, null);
                }
            }
            return result;
        }
    }

    private Session session(LogInvocation invocation) {
        try {
            Selection selection = resolver.resolve(invocation);
            return selection.policies.isEmpty() ? null : new Session(invocation, selection);
        } catch (RuntimeException error) {
            LOGGER.warn("Cannot resolve invocation logging policy for {}", invocation.getTarget());
            return null;
        }
    }

    private final class Session {
        private final LogInvocation invocation;
        private final Selection selection;
        private final String invocationId = UUID.randomUUID().toString().replace("-", "");
        private final String traceKey = properties.getLog().getTrace().getMdcKey();
        private final boolean traceEnabled = properties.getLog().getTrace().isEnabled();
        private final String traceId = TraceContext.get(traceKey);
        private final TraceData traceData = TraceContext.current();
        private final AtomicBoolean finished = new AtomicBoolean();
        private long start;

        private Session(LogInvocation invocation, Selection selection) {
            this.invocation = invocation;
            this.selection = selection;
        }

        private void entry() {
            arguments(InvocationLogFeature.ENTRY_ARGS);
            if (invocation.getSource() != InvocationLogSource.FEIGN) {
                headers(InvocationLogFeature.REQUEST_HEADERS, invocation.getRequestHeaders());
            }
            start = System.nanoTime();
        }

        private void finish(Object value, Throwable error) {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            long elapsed = System.nanoTime() - start;
            // 只包住框架自己的完成日志；对象转换和输出都使用入口 traceId。
            try (TraceContext.Scope scope = TraceContext.install(traceKey, traceData, traceEnabled)) {
                arguments(InvocationLogFeature.EXIT_ARGS);
                if (error == null) {
                    result(value);
                } else {
                    error(error);
                }
                if (invocation.getSource() == InvocationLogSource.FEIGN) {
                    // 实际请求头在底层请求构建后才可用；记录最终一次尝试，不重复输出。
                    headers(InvocationLogFeature.REQUEST_HEADERS, invocation.getRequestHeaders());
                }
                headers(InvocationLogFeature.RESPONSE_HEADERS, invocation.getResponseHeaders());
                slow(elapsed);
            }
        }

        private InvocationLogRecord record(InvocationLogFeature feature) {
            FeaturePolicy policy = selection.policies.get(feature);
            if (policy == null || !policy.enabled) {
                return null;
            }
            InvocationLogRecord record = new InvocationLogRecord();
            record.setFeature(feature);
            record.setSource(invocation.getSource());
            record.setTarget(invocation.getTarget());
            record.setLoggerName(invocation.getMethod().getDeclaringClass().getName());
            record.setLevel(policy.level);
            record.setTraceId(traceId);
            record.setInvocationId(invocationId);
            return InvocationLogSupport.enabled(writer, record) ? record : null;
        }

        private void arguments(InvocationLogFeature feature) {
            InvocationLogRecord record = record(feature);
            if (record != null && selection.maxLength != 0) {
                Map<String, Object> values =
                        InvocationLogSupport.arguments(
                                selection.metadata.parameterNames, invocation.getArguments());
                record.setPayload(InvocationLogSupport.format(values, formatter, selection.maxLength));
                write(record);
            }
        }

        private void result(Object value) {
            InvocationLogRecord record = record(InvocationLogFeature.EXIT_RESULT);
            if (record != null && selection.maxLength != 0) {
                String text =
                        selection.metadata.voidResult
                                ? InvocationLogSupport.VOID_RESULT
                                : InvocationLogSupport.formatResult(value, formatter, selection.maxLength);
                record.setPayload(text);
                write(record);
            }
        }

        private void error(Throwable throwable) {
            InvocationLogRecord record = record(InvocationLogFeature.ERROR_LOG);
            if (record != null) {
                Throwable error = unwrap(throwable);
                record.setErrorType(error.getClass().getName());
                record.setErrorMessage(error.getMessage());
                write(record);
            }
        }

        private void slow(long elapsed) {
            FeaturePolicy policy = selection.policies.get(InvocationLogFeature.SLOW_LOG);
            if (policy.threshold < 0
                    || !InvocationLogSupport.exceedsSlowThresholdNanos(elapsed, policy.threshold)) {
                return;
            }
            InvocationLogRecord record = record(InvocationLogFeature.SLOW_LOG);
            if (record != null) {
                record.setCostMs(TimeUnit.NANOSECONDS.toMillis(elapsed));
                record.setThresholdMs(policy.threshold);
                write(record);
            }
        }

        private void headers(
                InvocationLogFeature feature, Supplier<Map<String, List<String>>> supplier) {
            if (supplier == null || selection.maxLength == 0) {
                return;
            }
            InvocationLogRecord record = record(feature);
            if (record == null) {
                return;
            }
            try {
                Map<String, List<String>> values = supplier.get();
                if (values == null) {
                    return;
                }
                FeaturePolicy policy = selection.policies.get(feature);
                Map<String, List<String>> selected = new LinkedHashMap<>();
                for (Map.Entry<String, List<String>> entry : values.entrySet()) {
                    if (policy.allowlist.isEmpty()
                            || policy.allowlist.stream()
                            .anyMatch(name -> name.equalsIgnoreCase(entry.getKey()))) {
                        selected.put(entry.getKey(), entry.getValue());
                    }
                }
                record.setPayload(InvocationLogSupport.format(selected, formatter, selection.maxLength));
            } catch (RuntimeException error) {
                record.setPayload(InvocationLogSupport.SERIALIZATION_FAILED_PAYLOAD);
            }
            write(record);
        }

        private void write(InvocationLogRecord record) {
            InvocationLogSupport.safeWrite(writer, record);
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        for (int i = 0; i < 8; i++) {
            boolean wrapper =
                    current instanceof CompletionException
                            || current instanceof ExecutionException
                            || current instanceof InvocationTargetException
                            || current instanceof UndeclaredThrowableException;
            if (!wrapper || current.getCause() == null || current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return current;
    }
}
