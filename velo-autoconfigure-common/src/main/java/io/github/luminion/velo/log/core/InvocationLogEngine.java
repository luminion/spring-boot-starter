package io.github.luminion.velo.log.core;

import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.LogValueFormatter;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.core.InvocationLogPolicyResolver.FeaturePolicy;
import io.github.luminion.velo.log.core.InvocationLogPolicyResolver.Selection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 共用的同步方法调用生命周期；不等待异步完成，不管理链路上下文。
 */
public class InvocationLogEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(InvocationLogEngine.class);
    private final LogValueFormatter formatter;
    private final InvocationLogWriter writer;
    private final InvocationLogPolicyResolver resolver;

    public InvocationLogEngine(VeloProperties properties, LogValueFormatter formatter, InvocationLogWriter writer) {
        this.formatter = formatter;
        this.writer = writer;
        this.resolver = new InvocationLogPolicyResolver(properties);
    }

    public Object invoke(LogInvocation invocation, InvocationExecution execution) throws Throwable {
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
            session.finish(result, null);
        }
        return result;
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
            long elapsed = System.nanoTime() - start;
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
            return InvocationLogSupport.enabled(writer, record) ? record : null;
        }

        private void arguments(InvocationLogFeature feature) {
            InvocationLogRecord record = record(feature);
            if (record != null && selection.payloadEnabled) {
                Map<String, Object> values =
                        InvocationLogSupport.arguments(
                                selection.metadata.parameterNames, invocation.getArguments());
                record.setContent(InvocationLogSupport.format(values, formatter));
                write(record);
            }
        }

        private void result(Object value) {
            InvocationLogRecord record = record(InvocationLogFeature.EXIT_RESULT);
            if (record != null && selection.payloadEnabled) {
                String text =
                        selection.metadata.voidResult
                                ? InvocationLogSupport.VOID_RESULT
                                : InvocationLogSupport.formatResult(value, formatter);
                record.setContent(text);
                write(record);
            }
        }

        private void error(Throwable throwable) {
            InvocationLogRecord record = record(InvocationLogFeature.ERROR_LOG);
            if (record != null) {
                Throwable error = unwrap(throwable);
                Map<String, Object> content = new LinkedHashMap<>();
                content.put("type", error.getClass().getName());
                content.put("message", error.getMessage());
                record.setContent(InvocationLogSupport.format(content, formatter));
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
                // 耗时摘要使用固定格式，毫秒单位跟在数值后，不作为字段名或对象载荷序列化。
                long cost = TimeUnit.NANOSECONDS.toMillis(elapsed);
                record.setContent("{cost=" + cost + "ms, threshold=" + policy.threshold + "ms}");
                write(record);
            }
        }

        private void headers(
                InvocationLogFeature feature, Supplier<Map<String, List<String>>> supplier) {
            if (supplier == null || !selection.payloadEnabled) {
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
                record.setContent(InvocationLogSupport.format(selected, formatter));
            } catch (RuntimeException error) {
                record.setContent(InvocationLogSupport.SERIALIZATION_FAILED_PAYLOAD);
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
                    current instanceof InvocationTargetException
                            || current instanceof UndeclaredThrowableException;
            if (!wrapper || current.getCause() == null || current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return current;
    }
}
