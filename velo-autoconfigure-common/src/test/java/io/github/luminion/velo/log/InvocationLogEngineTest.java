package io.github.luminion.velo.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.annotation.EntryArgs;
import io.github.luminion.velo.log.annotation.ExitArgs;
import io.github.luminion.velo.log.annotation.ExitResult;
import io.github.luminion.velo.log.annotation.LogIgnore;
import io.github.luminion.velo.log.annotation.SlowLog;
import io.github.luminion.velo.log.trace.TraceContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.logging.LogLevel;

class InvocationLogEngineTest {
    private final VeloProperties properties = new VeloProperties();
    private final List<InvocationLogRecord> records = Collections.synchronizedList(new ArrayList<>());
    private final InvocationLogEngine engine =
            new InvocationLogEngine(properties, String::valueOf, records::add);

    @AfterEach
    void clear() {
        MDC.clear();
    }

    @Test
    void defaultsAreIndependentAndDoNotEmitEmptyCompletion() throws Throwable {
        for (InvocationLogSource source :
                new InvocationLogSource[]{
                        InvocationLogSource.CONTROLLER, InvocationLogSource.FEIGN, InvocationLogSource.INVOKE
                }) {
            records.clear();
            engine.invoke(call(Service.class, "call", source, "input"), () -> "result");
            assertThat(records.get(0).getFeature()).isEqualTo(InvocationLogFeature.ENTRY_ARGS);
            assertThat(records.get(0).getLevel()).isEqualTo(LogLevel.INFO);
            assertThat(records).hasSize(2);
            assertThat(records.get(1).getFeature()).isEqualTo(InvocationLogFeature.EXIT_RESULT);
            assertThat(records.get(1).getLevel()).isEqualTo(LogLevel.INFO);
            assertThat(records.get(1).getContent()).isEqualTo("result");
        }
    }

    @Test
    void methodOverridesClassAndSourceAndUsesAnnotationDefaults() throws Throwable {
        properties.getLog().getSources().getInvoke().getEntryArgs().setEnabled(false);
        engine.invoke(
                call(PolicyService.class, "enabled", InvocationLogSource.INVOKE, "input"), () -> "result");
        assertThat(records)
                .extracting(InvocationLogRecord::getFeature)
                .containsExactly(InvocationLogFeature.ENTRY_ARGS);
        assertThat(records.get(0).getLevel()).isEqualTo(LogLevel.INFO);
        records.clear();
        engine.invoke(
                call(PolicyService.class, "inherited", InvocationLogSource.INVOKE, "input"),
                () -> "result");
        assertThat(records).isEmpty();
    }

    @Test
    void ignoredMethodsDoNotCreateOrModifyTraceContext() throws Throwable {
        properties.getLog().getDefaults().getSlowLog().setThresholdMs(0L);
        for (Class<?> type : new Class<?>[]{Ignored.class, Service.class}) {
            String method = type == Ignored.class ? "call" : "ignored";
            engine.invoke(
                    call(type, method, InvocationLogSource.INVOKE, "input"),
                    () -> {
                        assertThat(MDC.get("traceId")).isNull();
                        assertThat(TraceContext.current()).isNull();
                        return "result";
                    });
        }
        assertThat(records).isEmpty();
    }

    @Test
    void slowDoesNotEnablePayloadOrPromoteOtherLevels() throws Throwable {
        properties.getLog().getSources().getController().getEntryArgs().setEnabled(false);
        properties.getLog().getSources().getController().getExitResult().setEnabled(false);
        properties.getLog().getDefaults().getSlowLog().setThresholdMs(0L);
        engine.invoke(
                call(Service.class, "call", InvocationLogSource.CONTROLLER, "input"), () -> "result");
        assertThat(records)
                .extracting(InvocationLogRecord::getFeature)
                .containsExactly(InvocationLogFeature.SLOW_LOG);
        assertThat(records.get(0).getLevel()).isEqualTo(LogLevel.WARN);
        assertThat(records.get(0).getContent()).contains("thresholdMs=0");
        records.clear();
        properties.getLog().getSources().getController().getEntryArgs().setEnabled(true);
        engine.invoke(
                call(Service.class, "call", InvocationLogSource.CONTROLLER, "input"), () -> "result");
        assertThat(records.get(0).getLevel()).isEqualTo(LogLevel.INFO);
        assertThat(records.get(1).getLevel()).isEqualTo(LogLevel.WARN);
    }

    @Test
    void slowAnnotationOverridesGlobalThresholdAndLevel() throws Throwable {
        properties.getLog().getDefaults().getSlowLog().setThresholdMs(600000L);
        engine.invoke(
                call(Service.class, "slow", InvocationLogSource.CONTROLLER, "input"), () -> "result");
        assertThat(records.get(2).getFeature()).isEqualTo(InvocationLogFeature.SLOW_LOG);
        assertThat(records.get(2).getContent()).contains("thresholdMs=0");
        assertThat(records.get(2).getLevel()).isEqualTo(LogLevel.INFO);
    }

    @Test
    void errorIsWarnSummaryAndOriginalExceptionContinues() throws Exception {
        IllegalStateException original = new IllegalStateException("failed\nmessage");
        assertThatThrownBy(
                () ->
                        engine.invoke(
                                call(Service.class, "call", InvocationLogSource.INVOKE, "input"),
                                () -> {
                                    throw original;
                                }))
                .isSameAs(original);
        assertThat(records)
                .extracting(InvocationLogRecord::getFeature)
                .containsExactly(InvocationLogFeature.ENTRY_ARGS, InvocationLogFeature.ERROR_LOG);
        assertThat(records.get(1).getLevel()).isEqualTo(LogLevel.WARN);
        assertThat(records.get(1).getContent())
                .contains(IllegalStateException.class.getName(), "failed\\nmessage");
    }

    @Test
    void errorCanBeDisabledWithoutChangingException() throws Exception {
        properties.getLog().getSources().getInvoke().getErrorLog().setEnabled(false);
        RuntimeException error = new RuntimeException("boom");
        assertThatThrownBy(
                () ->
                        engine.invoke(
                                call(Service.class, "call", InvocationLogSource.INVOKE, "input"),
                                () -> {
                                    throw error;
                                }))
                .isSameAs(error);
        assertThat(records).hasSize(1);
    }

    @Test
    void exitArgumentsObserveMutationAndAreOneSeparateLine() throws Throwable {
        StringBuilder value = new StringBuilder("before");
        Method method = Service.class.getMethod("mutate", StringBuilder.class);
        LogInvocation invocation =
                LogInvocation.builder()
                        .method(method)
                        .targetClass(Service.class)
                        .source(InvocationLogSource.INVOKE)
                        .target("mutate()")
                        .arguments(new Object[]{value})
                        .build();
        engine.invoke(
                invocation,
                () -> {
                    value.append("-after");
                    return null;
                });
        assertThat(records)
                .extracting(InvocationLogRecord::getFeature)
                .containsExactly(
                        InvocationLogFeature.ENTRY_ARGS,
                        InvocationLogFeature.EXIT_ARGS,
                        InvocationLogFeature.EXIT_RESULT);
        assertThat(records.get(0).getContent()).contains("before").doesNotContain("after");
        assertThat(records.get(1).getContent()).contains("before-after");
        assertThat(records.get(2).getContent()).isEqualTo("void");
    }

    @Test
    void httpResultWrapperLogsBodyAndKeepsOriginalResponse() throws Throwable {
        properties.getLog().getSources().getController().getExitResult().setEnabled(true);
        org.springframework.http.ResponseEntity<String> response =
                org.springframework.http.ResponseEntity.ok("body");
        Object returned =
                engine.invoke(
                        call(Service.class, "call", InvocationLogSource.CONTROLLER, "input"), () -> response);
        assertThat(returned).isSameAs(response);
        assertThat(records.get(1).getContent()).isEqualTo("body");
    }

    @Test
    void retainsFullVoidAndHeaderFailureMarkers() throws Throwable {
        properties.getLog().getSources().getController().getRequestHeaders().setEnabled(true);
        LogInvocation invocation = LogInvocation.builder()
                .method(Service.class.getMethod("mutate", StringBuilder.class))
                .targetClass(Service.class)
                .source(InvocationLogSource.CONTROLLER)
                .target("mutate()")
                .arguments(new Object[]{new StringBuilder("value")})
                .requestHeaders(() -> {
                    throw new IllegalStateException("header unavailable");
                })
                .build();
        assertThat(engine.invoke(invocation, () -> null)).isNull();
        assertThat(records).anySatisfy(record -> {
            assertThat(record.getFeature()).isEqualTo(InvocationLogFeature.REQUEST_HEADERS);
            assertThat(record.getContent()).isEqualTo("serialization-failed");
        });
        assertThat(records).anySatisfy(record -> {
            assertThat(record.getFeature()).isEqualTo(InvocationLogFeature.EXIT_RESULT);
            assertThat(record.getContent()).isEqualTo("void");
        });
    }

    @Test
    void returnsOriginalFutureAndRecordsExitBeforeItCompletes() throws Throwable {
        CompletableFuture<String> original = new CompletableFuture<>();
        Object returned =
                engine.invoke(
                        call(Service.class, "call", InvocationLogSource.INVOKE, "input"), () -> original);
        assertThat(returned).isSameAs(original);
        assertThat(original).isNotDone();
        assertThat(original.getNumberOfDependents()).isZero();
        assertThat(records)
                .extracting(InvocationLogRecord::getFeature)
                .containsExactly(InvocationLogFeature.ENTRY_ARGS, InvocationLogFeature.EXIT_RESULT);
        assertThat(original.cancel(true)).isTrue();
        assertThat(records).hasSize(2);
        assertThat(original.complete("late")).isFalse();
    }

    @Test
    void laterCompletionDoesNotWriteLogsOrInstallTraceOnAnotherThread() throws Throwable {
        List<String> seen = new ArrayList<>();
        InvocationLogEngine logging =
                new InvocationLogEngine(
                        properties,
                        String::valueOf,
                        record -> {
                            records.add(record);
                            seen.add(MDC.get("traceId"));
                            assertThat(TraceContext.current()).isNull();
                        });
        CompletableFuture<String> original = new CompletableFuture<>();
        MDC.put("traceId", "caller-trace");
        logging.invoke(
                call(Service.class, "call", InvocationLogSource.INVOKE, "input"), () -> original);
        AtomicReference<String> after = new AtomicReference<>();
        Thread worker =
                new Thread(
                        () -> {
                            MDC.put("traceId", "worker-trace");
                            original.complete("done");
                            after.set(MDC.get("traceId"));
                            MDC.clear();
                        });
        worker.start();
        worker.join();
        assertThat(seen).containsExactly("caller-trace", "caller-trace");
        assertThat(after.get()).isEqualTo("worker-trace");
        assertThat(records).hasSize(2);
        assertThat(MDC.get("traceId")).isEqualTo("caller-trace");
    }

    @Test
    void sourceConfigurationIsReadAgainAfterMetadataWasCached() throws Throwable {
        engine.invoke(
                call(Service.class, "call", InvocationLogSource.CONTROLLER, "input"), () -> "result");
        assertThat(records).hasSize(2);
        records.clear();
        properties.getLog().getSources().getController().getEntryArgs().setEnabled(false);
        properties.getLog().getSources().getController().getExitResult().setEnabled(false);
        engine.invoke(
                call(Service.class, "call", InvocationLogSource.CONTROLLER, "input"), () -> "result");
        assertThat(records).isEmpty();
    }

    @Test
    void filteredOrDisabledPayloadDoesNotCallFormatter() throws Throwable {
        AtomicInteger formatted = new AtomicInteger();
        InvocationLogWriter rejecting =
                new InvocationLogWriter() {
                    public void write(InvocationLogRecord record) {
                        throw new AssertionError();
                    }

                    public boolean isEnabled(InvocationLogRecord record) {
                        return false;
                    }
                };
        InvocationLogEngine logging =
                new InvocationLogEngine(
                        properties,
                        value -> {
                            formatted.incrementAndGet();
                            return "value";
                        },
                        rejecting);
        logging.invoke(call(Service.class, "call", InvocationLogSource.INVOKE, "input"), () -> "done");
        assertThat(formatted).hasValue(0);
        properties.getLog().getDefaults().setMaxPayloadLength(0);
        engine.invoke(call(Service.class, "call", InvocationLogSource.INVOKE, "input"), () -> "done");
        assertThat(records).isEmpty();
    }

    @Test
    void formatterAndWriterFailuresDoNotChangeBusinessOutcome() throws Throwable {
        InvocationLogEngine logging =
                new InvocationLogEngine(
                        properties,
                        value -> {
                            throw new IllegalArgumentException();
                        },
                        record -> {
                            throw new IllegalStateException();
                        });
        Object expected = new Object();
        assertThat(
                logging.invoke(
                        call(Service.class, "call", InvocationLogSource.INVOKE, "input"), () -> expected))
                .isSameAs(expected);
        RuntimeException error = new RuntimeException("original");
        assertThatThrownBy(
                () ->
                        logging.invoke(
                                call(Service.class, "call", InvocationLogSource.INVOKE, "input"),
                                () -> {
                                    throw error;
                                }))
                .isSameAs(error);
    }

    @Test
    void exceptionSummaryRemainsCompleteWhenPayloadIsDisabledOrUnlimited() {
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            message.append('x');
        }
        RuntimeException original = new RuntimeException(message.toString());
        for (int limit : new int[]{0, -1}) {
            records.clear();
            properties.getLog().getDefaults().setMaxPayloadLength(limit);
            assertThatThrownBy(() -> engine.invoke(
                    call(Service.class, "call", InvocationLogSource.INVOKE, "input"), () -> {
                        throw original;
                    })).isSameAs(original);
            InvocationLogRecord error = records.get(records.size() - 1);
            assertThat(error.getFeature()).isEqualTo(InvocationLogFeature.ERROR_LOG);
            assertThat(error.getContent()).contains(message.toString()).doesNotEndWith("...");
            if (limit == 0) {
                assertThat(records).hasSize(1);
            }
        }
    }

    @Test
    void sourceSwitchAndIgnoreDoNotSuppressNestedIndependentCalls() throws Throwable {
        properties.getLog().getSources().getController().setEnabled(false);
        engine.invoke(
                call(Service.class, "call", InvocationLogSource.CONTROLLER, "input"),
                () ->
                        engine.invoke(
                                call(Service.class, "call", InvocationLogSource.INVOKE, "nested"), () -> "done"));
        assertThat(records).hasSize(2);
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    void alreadyFailedFutureIsStillARegularReturnedObject() throws Throwable {
        CompletableFuture<String> original = new CompletableFuture<>();
        original.completeExceptionally(new IllegalArgumentException("bad"));
        assertThat(
                engine.invoke(
                        call(Service.class, "call", InvocationLogSource.INVOKE, "input"), () -> original))
                .isSameAs(original);
        assertThat(records)
                .extracting(InvocationLogRecord::getFeature)
                .containsExactly(InvocationLogFeature.ENTRY_ARGS, InvocationLogFeature.EXIT_RESULT);
    }

    @Test
    void synchronousReflectionFailureLogsUnderlyingTypeAndRethrowsOriginal() throws Exception {
        InvocationTargetException original =
                new InvocationTargetException(new IllegalArgumentException("bad"));
        assertThatThrownBy(
                () ->
                        engine.invoke(
                                call(Service.class, "call", InvocationLogSource.INVOKE, "input"),
                                () -> {
                                    throw original;
                                }))
                .isSameAs(original);
        assertThat(records.get(1).getContent())
                .contains("type=java.lang.IllegalArgumentException", "message=bad");
    }

    private LogInvocation call(Class<?> type, String name, InvocationLogSource source, String value)
            throws Exception {
        return LogInvocation.builder()
                .method(type.getMethod(name, String.class))
                .targetClass(type)
                .source(source)
                .target(name + "()")
                .arguments(new Object[]{value})
                .build();
    }

    static class Service {
        public String call(String value) {
            return value;
        }

        @LogIgnore
        public String ignored(String value) {
            return value;
        }

        @SlowLog(thresholdMs = 0, level = LogLevel.INFO)
        public String slow(String value) {
            return value;
        }

        @ExitArgs
        public void mutate(StringBuilder value) {
        }
    }

    @EntryArgs(enabled = false, level = LogLevel.DEBUG)
    @ExitResult(enabled = false)
    static class PolicyService {
        @EntryArgs
        public String enabled(String value) {
            return value;
        }

        public String inherited(String value) {
            return value;
        }
    }

    @LogIgnore
    static class Ignored {
        @EntryArgs
        @ExitResult
        public String call(String value) {
            return value;
        }
    }
}
