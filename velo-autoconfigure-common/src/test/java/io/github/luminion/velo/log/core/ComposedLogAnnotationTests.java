package io.github.luminion.velo.log.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.annotation.*;
import io.github.luminion.velo.log.annotation.composed.InvokeLog;
import io.github.luminion.velo.log.aspect.InvokeLogAspect;
import io.github.luminion.velo.log.model.InvocationLogFeature;
import io.github.luminion.velo.log.model.InvocationLogRecord;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.model.LogPolicyLevel;
import java.lang.annotation.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.annotation.AliasFor;

/**
 * 验证两种代理下的组合匹配、直接声明优先、默认套餐及 Spring 别名。
 */
class ComposedLogAnnotationTests {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void nestedInterfaceCompositionActivatesOnlyItsFeatures(boolean classProxy) {
        List<InvocationLogRecord> records = new ArrayList<>();
        Operations calls = proxy(classProxy, records);
        assertThat(calls.measured("input")).isEqualTo("input");
        assertThat(records).extracting(InvocationLogRecord::getFeature).containsExactlyInAnyOrder(
                InvocationLogFeature.ENTRY_ARGS, InvocationLogFeature.COST_LOG, InvocationLogFeature.SLOW_LOG);
        assertThat(records).allSatisfy(record ->
                assertThat(record.getSource()).isEqualTo(InvocationLogSource.ANNOTATION));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void aliasesUseBothDeclaredArgumentsAndCompositionDefaults(boolean classProxy) {
        List<InvocationLogRecord> records = new ArrayList<>();
        Operations calls = proxy(classProxy, records);
        calls.aliased("input");
        assertThat(records).singleElement().satisfies(record -> {
            assertThat(record.getLevel()).isEqualTo(LogPolicyLevel.TRACE);
            assertThat(record.getContent()).endsWith("threshold=0ms}");
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void directDisabledDeclarationWinsOverCompositionInEitherOrder(boolean classProxy) {
        List<InvocationLogRecord> records = new ArrayList<>();
        Operations calls = proxy(classProxy, records);
        calls.directFirst("input");
        calls.composedFirst("input");
        assertThat(records).extracting(InvocationLogRecord::getFeature).doesNotContain(InvocationLogFeature.SLOW_LOG);
        assertThat(records).hasSize(4);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void invokeLogIsAnOrdinaryCompositionAndIgnoreStillWins(boolean classProxy) {
        List<InvocationLogRecord> records = new ArrayList<>();
        Operations calls = proxy(classProxy, records);
        calls.profile("input");
        assertThat(records).extracting(InvocationLogRecord::getFeature).containsExactly(
                InvocationLogFeature.ENTRY_ARGS, InvocationLogFeature.EXIT_RESULT, InvocationLogFeature.COST_LOG);
        records.clear();
        calls.ignored("input");
        assertThat(records).isEmpty();
    }

    private Operations proxy(boolean classProxy, List<InvocationLogRecord> records) {
        InvocationLogEngine engine = new InvocationLogEngine(new VeloProperties(), String::valueOf, records::add);
        ProxyFactory factory = new ProxyFactory(new Service());
        factory.setInterfaces(Operations.class);
        factory.setProxyTargetClass(classProxy);
        factory.addAdvisor(new InvokeLogAspect(engine));
        return (Operations) factory.getProxy();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @EntryArgs
    @CostLog(level = LogPolicyLevel.DEBUG)
    @SlowLog(threshold = 0)
    @interface Measured {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Measured
    @interface Nested {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @SlowLog
    @interface Aliased {
        @AliasFor(annotation = SlowLog.class, attribute = "threshold")
        long threshold() default 0;
        @AliasFor(annotation = SlowLog.class, attribute = "level")
        LogPolicyLevel level() default LogPolicyLevel.WARN;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @InvokeLog
    @interface Profile {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @LogIgnore
    @interface NoLog {
    }

    interface Operations {
        @Nested
        String measured(String input);
        String aliased(String input);
        String directFirst(String input);
        String composedFirst(String input);
        String profile(String input);
        String ignored(String input);
    }

    static class Service implements Operations {
        public String measured(String input) { return input; }

        @Aliased(level = LogPolicyLevel.TRACE)
        public String aliased(String input) { return input; }

        @SlowLog(level = LogPolicyLevel.OFF)
        @Nested
        public String directFirst(String input) { return input; }

        @Nested
        @SlowLog(level = LogPolicyLevel.OFF)
        public String composedFirst(String input) { return input; }

        @Profile
        public String profile(String input) { return input; }

        @NoLog
        @Measured
        public String ignored(String input) { return input; }
    }
}
