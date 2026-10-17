package io.github.luminion.velo.log.core;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.core.InvocationLogPolicyResolver.Selection;
import io.github.luminion.velo.log.formatter.DefaultInvocationLogContextFormatter;
import io.github.luminion.velo.log.handler.DefaultInvocationLogHandlers;
import io.github.luminion.velo.log.model.InvocationLogContext;
import io.github.luminion.velo.log.model.InvocationLogEvent;
import io.github.luminion.velo.log.model.InvocationLogFeature;
import io.github.luminion.velo.log.model.InvocationLogPolicy;
import io.github.luminion.velo.log.model.InvocationLogRecord;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.model.LogInvocation;
import io.github.luminion.velo.log.model.LogPolicyLevel;
import io.github.luminion.velo.log.spi.InvocationLogContextFormatter;
import io.github.luminion.velo.log.spi.InvocationLogHandler;
import io.github.luminion.velo.log.spi.InvocationLogWriter;
import io.github.luminion.velo.log.spi.LogValueFormatter;
import io.github.luminion.velo.log.support.InvocationLogSupport;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 共用调用协调器：业务只执行一次，统一计时和阶段顺序；功能内容由独立处理器生成。
 */
public class InvocationLogEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(InvocationLogEngine.class);
    private final LogValueFormatter formatter;
    private final InvocationLogWriter writer;
    private final InvocationLogPolicyResolver resolver;
    private final Map<InvocationLogFeature, InvocationLogHandler> handlers = new EnumMap<>(InvocationLogFeature.class);
    private final Map<InvocationLogSource, InvocationLogContextFormatter> contexts = new EnumMap<>(InvocationLogSource.class);

    public InvocationLogEngine(VeloProperties properties, LogValueFormatter formatter, InvocationLogWriter writer) {
        this(properties, formatter, writer, Collections.emptyList(), Collections.emptyList());
    }

    public InvocationLogEngine(VeloProperties properties, LogValueFormatter formatter, InvocationLogWriter writer,
            Collection<? extends InvocationLogHandler> customHandlers,
            Collection<? extends InvocationLogContextFormatter> customContexts) {
        this.formatter = Objects.requireNonNull(formatter, "formatter");
        this.writer = Objects.requireNonNull(writer, "writer");
        this.resolver = new InvocationLogPolicyResolver(properties);
        for (InvocationLogHandler handler : DefaultInvocationLogHandlers.create()) {
            handlers.put(handler.getFeature(), handler);
        }
        Set<InvocationLogFeature> features = new HashSet<>();
        for (InvocationLogHandler handler : customHandlers) {
            InvocationLogFeature feature = Objects.requireNonNull(handler.getFeature(), "handler feature");
            if (!features.add(feature)) {
                throw new IllegalArgumentException("Multiple custom log handlers for " + feature);
            }
            handlers.put(feature, handler);
        }
        for (InvocationLogSource source : InvocationLogSource.values()) {
            contexts.put(source, new DefaultInvocationLogContextFormatter(source));
        }
        Set<InvocationLogSource> sources = new HashSet<>();
        for (InvocationLogContextFormatter context : customContexts) {
            InvocationLogSource source = Objects.requireNonNull(context.getSource(), "context source");
            if (!sources.add(source)) {
                throw new IllegalArgumentException("Multiple custom log context formatters for " + source);
            }
            contexts.put(source, context);
        }
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
            LOGGER.warn("Cannot resolve invocation logging policy for {}", invocation.getMethod().getName());
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

        private InvocationLogEvent event(Object result, Throwable error, long elapsed) {
            return InvocationLogEvent.builder().invocation(invocation)
                    .parameterNames(selection.metadata.parameterNames).voidResult(selection.metadata.voidResult)
                    .result(result).error(error).elapsedNanos(elapsed).build();
        }

        private void entry() {
            InvocationLogEvent event = event(null, null, 0);
            emit(InvocationLogFeature.ENTRY_ARGS, event);
            if (invocation.getSource() != InvocationLogSource.FEIGN) {
                emit(InvocationLogFeature.REQUEST_HEADERS, event);
            }
            start = System.nanoTime();
        }

        private void finish(Object result, Throwable error) {
            long elapsed = System.nanoTime() - start;
            InvocationLogEvent event = event(result, error, elapsed);
            emit(InvocationLogFeature.EXIT_ARGS, event);
            if (error == null) {
                emit(InvocationLogFeature.EXIT_RESULT, event);
            } else {
                emit(InvocationLogFeature.ERROR_LOG, event);
            }
            if (invocation.getSource() == InvocationLogSource.FEIGN) {
                // Feign 请求头在请求构建之后才完整，继续只输出最终一次尝试。
                emit(InvocationLogFeature.REQUEST_HEADERS, event);
            }
            emit(InvocationLogFeature.RESPONSE_HEADERS, event);
            emit(InvocationLogFeature.COST_LOG, event);
            emit(InvocationLogFeature.SLOW_LOG, event);
        }

        private void emit(InvocationLogFeature feature, InvocationLogEvent event) {
            InvocationLogPolicy policy = selection.policies.get(feature);
            if (policy == null || policy.getLevel() == LogPolicyLevel.OFF || policy.getMaxLength() == 0) {
                return;
            }
            InvocationLogRecord record = new InvocationLogRecord();
            record.setSource(invocation.getSource());
            record.setTarget(invocation.getMethod().getName() + "()");
            record.setLoggerName(invocation.getMethod().getDeclaringClass().getName());
            record.setFeature(feature);
            record.setLevel(policy.getLevel());
            if (!InvocationLogSupport.enabled(writer, record)) {
                return;
            }
            try {
                String content = handlers.get(feature).render(event, policy, formatter);
                if (content == null) {
                    return;
                }
                String singleLine = InvocationLogSupport.singleLine(content);
                String limitedContent = limit(singleLine, policy.getMaxLength());
                record.setContent(limitedContent);
            } catch (RuntimeException failure) {
                LOGGER.warn("Invocation log handler failed for {}: {}", feature, invocation.getMethod().getName());
                return;
            }
            publicContext(record);
            InvocationLogSupport.safeWrite(writer, record);
        }

        private void publicContext(InvocationLogRecord record) {
            try {
                InvocationLogContext context = invocation.getContext() == null ? null : invocation.getContext().get();
                record.setContext(context);
                String text = contexts.get(invocation.getSource()).format(invocation, context);
                record.setContextContent(text);
            } catch (RuntimeException failure) {
                LOGGER.warn("Invocation log context formatting failed for {}", invocation.getMethod().getName());
            }
        }
    }

    private static String limit(String content, int maxLength) {
        if (maxLength < 0 || content.length() <= maxLength) {
            return content;
        }
        int end = maxLength;
        if (end > 0 && Character.isHighSurrogate(content.charAt(end - 1))) {
            end--;
        }
        return content.substring(0, end);
    }
}
