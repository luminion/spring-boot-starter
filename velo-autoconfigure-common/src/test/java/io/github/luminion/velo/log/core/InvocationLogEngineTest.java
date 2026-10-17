package io.github.luminion.velo.log.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.annotation.*;
import io.github.luminion.velo.log.annotation.composed.InvokeLog;
import io.github.luminion.velo.log.handler.CostLogHandler;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * 通过协调器与输出记录验证生命周期、独立功能及可替换处理器。
 */
class InvocationLogEngineTest {
    private final VeloProperties properties = new VeloProperties();
    private final List<InvocationLogRecord> records = new ArrayList<>();
    private final InvocationLogEngine engine = new InvocationLogEngine(properties, String::valueOf, records::add);

    @Test
    void defaultProfileOutputsArgumentsResultAndCost() throws Throwable {
        for (InvocationLogSource source : new InvocationLogSource[]{
                InvocationLogSource.CONTROLLER, InvocationLogSource.FEIGN, InvocationLogSource.ANNOTATION}) {
            records.clear();
            Object result = engine.invoke(call("profile", source), () -> "result");
            assertThat(result).isEqualTo("result");
            assertThat(records).extracting(InvocationLogRecord::getFeature).containsExactly(
                    InvocationLogFeature.ENTRY_ARGS, InvocationLogFeature.EXIT_RESULT, InvocationLogFeature.COST_LOG);
            assertThat(records).allSatisfy(record -> assertThat(record.getTarget()).isEqualTo("profile()"));
        }
    }

    @Test
    void costAndSlowUseOneClockWithoutSerializingPayloads() throws Throwable {
        InvocationLogEngine logging = new InvocationLogEngine(properties, value -> {
            throw new AssertionError("Timing summaries must not serialize payloads");
        }, records::add);
        AtomicInteger executions = new AtomicInteger();
        logging.invoke(call("timing", InvocationLogSource.ANNOTATION), executions::incrementAndGet);
        assertThat(executions).hasValue(1);
        assertThat(records).extracting(InvocationLogRecord::getFeature)
                .containsExactly(InvocationLogFeature.COST_LOG, InvocationLogFeature.SLOW_LOG);
        assertThat(records.get(1).getContent())
                .isEqualTo("{cost=" + records.get(0).getContent() + ", threshold=0ms}");
    }

    @Test
    void errorSummaryKeepsOriginalExceptionAndDoesNotProduceAResult() throws Exception {
        IllegalStateException original = new IllegalStateException("failed\nmessage");
        assertThatThrownBy(() -> engine.invoke(call("profile", InvocationLogSource.ANNOTATION), () -> {
            throw original;
        })).isSameAs(original);
        assertThat(records).extracting(InvocationLogRecord::getFeature).containsExactly(
                InvocationLogFeature.ENTRY_ARGS, InvocationLogFeature.ERROR_LOG, InvocationLogFeature.COST_LOG);
        assertThat(records.get(1).getLevel()).isEqualTo(LogPolicyLevel.WARN);
        assertThat(records.get(1).getContent()).contains("failed\\nmessage", IllegalStateException.class.getName());
    }

    @Test
    void wrappedErrorsAreUnwrappedOnlyForTheSummary() throws Exception {
        InvocationTargetException wrapper = new InvocationTargetException(new IllegalArgumentException("business"));
        assertThatThrownBy(() -> engine.invoke(call("profile", InvocationLogSource.ANNOTATION), () -> {
            throw wrapper;
        })).isSameAs(wrapper);
        assertThat(records.get(1).getContent()).contains(IllegalArgumentException.class.getName(), "business");
    }

    @Test
    void futureIsReturnedWithoutWaitingForItsCompletion() throws Throwable {
        CompletableFuture<String> future = new CompletableFuture<>();
        Object result = engine.invoke(call("profile", InvocationLogSource.ANNOTATION), () -> future);
        assertThat(result).isSameAs(future);
        assertThat(future).isNotDone();
        assertThat(records).hasSize(3);
        future.completeExceptionally(new IllegalStateException("later"));
        assertThat(records).hasSize(3);
    }

    @Test
    void argumentSnapshotsAreFormattedAtTheirOwnPhases() throws Throwable {
        StringBuilder value = new StringBuilder("before");
        LogInvocation invocation = call("arguments", InvocationLogSource.ANNOTATION).toBuilder()
                .arguments(new Object[]{value}).build();
        engine.invoke(invocation, () -> {
            value.append("-after");
            return "ok";
        });
        assertThat(records.get(0).getContent()).contains("before").doesNotContain("after");
        assertThat(records.get(1).getContent()).contains("before-after");
    }

    @Test
    void zeroLengthSkipsFormattingAndPositiveLengthTruncatesOnlyItsOwnFeature() throws Throwable {
        AtomicInteger formatted = new AtomicInteger();
        InvocationLogEngine logging = new InvocationLogEngine(properties, value -> {
            formatted.incrementAndGet();
            return "abcdefghij";
        }, records::add);
        logging.invoke(call("limited", InvocationLogSource.ANNOTATION), () -> "result");
        assertThat(formatted).hasValue(1);
        assertThat(records).extracting(InvocationLogRecord::getContent).containsExactly("abcd");
    }

    @Test
    void disabledWriterDoesNotRunAHandlerOrFormatter() throws Throwable {
        InvocationLogWriter disabled = new InvocationLogWriter() {
            @Override
            public boolean isEnabled(InvocationLogRecord record) {
                return false;
            }
            @Override
            public void write(InvocationLogRecord record) {
                throw new AssertionError("Filtered record must not be written");
            }
        };
        InvocationLogEngine logging = new InvocationLogEngine(properties, value -> {
            throw new AssertionError("Filtered payload must not be formatted");
        }, disabled);
        logging.invoke(call("profile", InvocationLogSource.ANNOTATION), () -> "ok");
    }

    @Test
    void customHandlerReplacesOnlyOneFunctionAndContextFormatterOnlyOneSource() throws Throwable {
        InvocationLogHandler cost = new CostLogHandler() {
            @Override
            public String render(InvocationLogEvent event, InvocationLogPolicy policy, LogValueFormatter formatter) {
                return "custom-cost";
            }
        };
        InvocationLogContextFormatter context = new InvocationLogContextFormatter() {
            @Override
            public InvocationLogSource getSource() {
                return InvocationLogSource.CONTROLLER;
            }
            @Override
            public String format(LogInvocation invocation, InvocationLogContext context) {
                return "tenant=demo method=" + invocation.getMethod().getName();
            }
        };
        InvocationLogEngine logging = new InvocationLogEngine(properties, String::valueOf, records::add,
                Collections.singletonList(cost), Collections.singletonList(context));
        logging.invoke(call("profile", InvocationLogSource.CONTROLLER), () -> "result");
        assertThat(records).hasSize(3).allSatisfy(record ->
                assertThat(record.getContextContent()).isEqualTo("tenant=demo method=profile"));
        assertThat(records.get(1).getContent()).isEqualTo("result");
        assertThat(records.get(2).getContent()).isEqualTo("custom-cost");
        records.clear();
        logging.invoke(call("profile", InvocationLogSource.ANNOTATION), () -> "result");
        assertThat(records).allSatisfy(record -> assertThat(record.getContextContent()).isEmpty());
    }

    @Test
    void extensionFailuresDoNotChangeBusinessResultOrStopOtherFeatures() throws Throwable {
        InvocationLogHandler failing = new CostLogHandler() {
            @Override
            public String render(InvocationLogEvent event, InvocationLogPolicy policy, LogValueFormatter formatter) {
                throw new IllegalStateException("handler");
            }
        };
        InvocationLogContextFormatter failingContext = new InvocationLogContextFormatter() {
            @Override
            public InvocationLogSource getSource() {
                return InvocationLogSource.ANNOTATION;
            }
            @Override
            public String format(LogInvocation invocation, InvocationLogContext context) {
                throw new IllegalStateException("context");
            }
        };
        InvocationLogEngine logging = new InvocationLogEngine(properties, String::valueOf, records::add,
                Collections.singletonList(failing), Collections.singletonList(failingContext));
        assertThat(logging.invoke(call("profile", InvocationLogSource.ANNOTATION), () -> "ok")).isEqualTo("ok");
        assertThat(records).extracting(InvocationLogRecord::getFeature)
                .containsExactly(InvocationLogFeature.ENTRY_ARGS, InvocationLogFeature.EXIT_RESULT);
    }

    @Test
    void headersKeepNullEmptyFailureAndFeignTimingRules() throws Throwable {
        LogInvocation invocation = call("headers", InvocationLogSource.FEIGN).toBuilder()
                .requestHeaders(Collections::emptyMap).responseHeaders(() -> {
                    throw new IllegalStateException("headers");
                }).build();
        engine.invoke(invocation, () -> "result");
        assertThat(records).extracting(InvocationLogRecord::getFeature).containsExactly(
                InvocationLogFeature.ENTRY_ARGS, InvocationLogFeature.EXIT_RESULT, InvocationLogFeature.REQUEST_HEADERS,
                InvocationLogFeature.RESPONSE_HEADERS, InvocationLogFeature.COST_LOG);
        assertThat(records.get(2).getContent()).isEqualTo("{}");
        assertThat(records.get(3).getContent()).isEqualTo(InvocationLogSupport.SERIALIZATION_FAILED_PAYLOAD);
        records.clear();
        engine.invoke(call("onlyHeaders", InvocationLogSource.ANNOTATION), () -> "ok");
        assertThat(records).isEmpty();
    }

    @Test
    void contextSnapshotsCanGainInstanceInformationAfterExecution() throws Throwable {
        AtomicInteger stage = new AtomicInteger();
        LogInvocation invocation = call("profile", InvocationLogSource.FEIGN).toBuilder()
                .context(() -> InvocationLogContext.builder().contextId("stock")
                        .instanceAddress(stage.get() == 0 ? null : "10.0.0.1:8080").build()).build();
        engine.invoke(invocation, () -> {
            stage.incrementAndGet();
            return "result";
        });
        assertThat(records.get(0).getContextContent()).isEqualTo("contextId=stock");
        assertThat(records.get(1).getContextContent()).isEqualTo("contextId=stock instance=10.0.0.1:8080");
        assertThat(records.get(0).getContext().getInstanceAddress()).isNull();
    }

    @Test
    void duplicateExtensionKeysFailInsteadOfDependingOnBeanOrder() {
        CostLogHandler first = new CostLogHandler();
        assertThatThrownBy(() -> new InvocationLogEngine(properties, String::valueOf, records::add,
                Arrays.asList(first, new CostLogHandler()), Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Multiple custom log handlers");
    }

    @Test
    void ignoreAndTotalSwitchesWinOverExplicitFeatures() throws Throwable {
        engine.invoke(call("ignored", InvocationLogSource.ANNOTATION), () -> "ok");
        properties.getLog().getSources().getAnnotation().setEnabled(false);
        engine.invoke(call("profile", InvocationLogSource.ANNOTATION), () -> "ok");
        properties.getLog().setEnabled(false);
        engine.invoke(call("profile", InvocationLogSource.CONTROLLER), () -> "ok");
        assertThat(records).isEmpty();
    }

    private LogInvocation call(String name, InvocationLogSource source) throws Exception {
        Method method = Service.class.getMethod(name, Object.class);
        return LogInvocation.builder().method(method).targetClass(Service.class)
                .source(source).arguments(new Object[]{"input"}).build();
    }

    static class Service {
        @InvokeLog
        public Object profile(Object input) { return input; }

        @CostLog
        @SlowLog(threshold = 0)
        public Object timing(Object input) { return input; }

        @EntryArgs
        @ExitArgs
        public Object arguments(Object input) { return input; }

        @EntryArgs(maxLength = 0)
        @ExitResult(maxLength = 4)
        public Object limited(Object input) { return input; }

        @RequestHeadersLog
        @ResponseHeadersLog
        public Object onlyHeaders(Object input) { return input; }

        @InvokeLog
        @RequestHeadersLog
        @ResponseHeadersLog
        public Object headers(Object input) { return input; }

        @InvokeLog
        @LogIgnore
        public Object ignored(Object input) { return input; }
    }
}
