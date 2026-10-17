package io.github.luminion.velo.log.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.annotation.EntryArgs;
import io.github.luminion.velo.log.annotation.ExitResult;
import io.github.luminion.velo.log.model.InvocationLogFeature;
import io.github.luminion.velo.log.model.InvocationLogRecord;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.model.LogInvocation;
import io.github.luminion.velo.log.model.LogPolicyLevel;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * 从实际日志记录验证完整功能声明，不依赖内部解析结构。
 */
class CompleteFeatureDeclarationTests {
    @Test
    void offSkipsFormattingAndDisablesOnlyItsOwnFeature() throws Throwable {
        List<InvocationLogRecord> records = new ArrayList<>();
        AtomicInteger formatted = new AtomicInteger();
        InvocationLogEngine engine = new InvocationLogEngine(new VeloProperties(), value -> {
            formatted.incrementAndGet();
            return String.valueOf(value);
        }, records::add);
        Method method = Selective.class.getMethod("call", String.class);
        LogInvocation invocation = LogInvocation.builder().method(method).targetClass(Selective.class)
                .source(InvocationLogSource.ANNOTATION).arguments(new Object[]{"input"}).build();

        Object result = engine.invoke(invocation, () -> "result");

        assertThat(result).isEqualTo("result");
        assertThat(formatted).hasValue(1);
        assertThat(records).singleElement().satisfies(record -> {
            assertThat(record.getFeature()).isEqualTo(InvocationLogFeature.EXIT_RESULT);
            assertThat(record.getLevel()).isEqualTo(LogPolicyLevel.INFO);
            assertThat(record.getContent()).isEqualTo("result");
        });
    }

    @Test
    void methodDeclarationUsesItsOwnDefaultLevelInsteadOfTheClassLevel() throws Throwable {
        List<InvocationLogRecord> records = new ArrayList<>();
        InvocationLogEngine engine = new InvocationLogEngine(new VeloProperties(), String::valueOf, records::add);
        Method method = Service.class.getMethod("call", String.class);
        LogInvocation invocation = LogInvocation.builder().method(method).targetClass(Service.class)
                .source(InvocationLogSource.ANNOTATION).arguments(new Object[]{"input"}).build();

        Object result = engine.invoke(invocation, () -> "result");

        assertThat(result).isEqualTo("result");
        assertThat(records).singleElement().satisfies(record -> {
            assertThat(record.getFeature()).isEqualTo(InvocationLogFeature.ENTRY_ARGS);
            assertThat(record.getLevel()).isEqualTo(LogPolicyLevel.INFO);
        });
    }

    @EntryArgs(level = LogPolicyLevel.OFF)
    static class Service {
        @EntryArgs
        public String call(String input) {
            return input;
        }
    }

    @EntryArgs
    @ExitResult
    static class Selective {
        @EntryArgs(level = LogPolicyLevel.OFF)
        public String call(String input) {
            return input;
        }
    }
}
