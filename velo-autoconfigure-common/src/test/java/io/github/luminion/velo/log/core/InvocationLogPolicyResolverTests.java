package io.github.luminion.velo.log.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.annotation.*;
import io.github.luminion.velo.log.handler.SlowLogHandler;
import io.github.luminion.velo.log.model.InvocationLogEvent;
import io.github.luminion.velo.log.model.InvocationLogFeature;
import io.github.luminion.velo.log.model.InvocationLogPolicy;
import io.github.luminion.velo.log.model.InvocationLogRecord;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.model.LogInvocation;
import io.github.luminion.velo.log.model.LogPolicyLevel;
import io.github.luminion.velo.log.spi.LogValueFormatter;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AliasFor;

/**
 * 从公共协调器和处理器接口验证完整声明及默认组合选择。
 */
class InvocationLogPolicyResolverTests {
    private final VeloProperties properties = new VeloProperties();
    private final List<InvocationLogRecord> records = new ArrayList<>();

    @Test
    void methodUsesCompleteDefaultsAndKeepsOtherClassFeatures() throws Throwable {
        InvocationLogEngine engine = engine();
        engine.invoke(call(Layered.class, "call", InvocationLogSource.ANNOTATION), () -> "ok");
        assertThat(records).extracting(InvocationLogRecord::getFeature)
                .containsExactly(InvocationLogFeature.ENTRY_ARGS, InvocationLogFeature.SLOW_LOG);
        assertThat(records.get(1).getContent()).isEqualTo("DEBUG:1000");
    }

    @Test
    void configuredDefaultCompositionSelectsOnlyItsFeatures() throws Throwable {
        properties.getLog().getSources().getController().setDefaultAnnotation(TimingPreset.class);
        InvocationLogEngine engine = engine();
        engine.invoke(call(Plain.class, "call", InvocationLogSource.CONTROLLER), () -> "ok");
        assertThat(records).extracting(InvocationLogRecord::getFeature)
                .containsExactly(InvocationLogFeature.COST_LOG, InvocationLogFeature.SLOW_LOG);
        assertThat(records.get(1).getContent()).isEqualTo("WARN:200");
    }

    @Test
    void explicitDeclarationCompletelyOverridesConfiguredPreset() throws Throwable {
        properties.getLog().getSources().getController().setDefaultAnnotation(TimingPreset.class);
        engine().invoke(call(Explicit.class, "call", InvocationLogSource.CONTROLLER), () -> "ok");
        assertThat(records.get(1).getContent()).isEqualTo("DEBUG:1000");
    }

    @Test
    void annotationCacheDoesNotFreezeDefaultCompositionSelection() throws Throwable {
        InvocationLogEngine engine = engine();
        LogInvocation call = call(Plain.class, "call", InvocationLogSource.CONTROLLER);
        engine.invoke(call, () -> "ok");
        assertThat(records).hasSize(4);
        records.clear();
        properties.getLog().getSources().getController().setDefaultAnnotation(TimingPreset.class);
        engine.invoke(call, () -> "ok");
        assertThat(records).hasSize(2);
    }

    @Test
    void independentAtomicEntryDoesNotAcquireAnImplicitProfile() throws Throwable {
        engine().invoke(call(Single.class, "call", InvocationLogSource.ANNOTATION), () -> "ok");
        assertThat(records).extracting(InvocationLogRecord::getFeature).containsExactly(InvocationLogFeature.COST_LOG);
    }

    @Test
    void configuredCompositionUsesItsAliasDefaults() throws Throwable {
        properties.getLog().getSources().getController().setDefaultAnnotation(AliasedPreset.class);
        engine().invoke(call(Plain.class, "call", InvocationLogSource.CONTROLLER), () -> "ok");
        assertThat(records).singleElement().satisfies(record ->
                assertThat(record.getContent()).isEqualTo("WARN:250"));
    }

    @Test
    void invalidDefaultCompositionsFailAtConstruction() {
        for (Class<? extends Annotation> type : Arrays.asList(NoFunctions.class, Required.class, InvalidThreshold.class)) {
            properties.getLog().getSources().getController().setDefaultAnnotation(type);
            assertThatThrownBy(this::engine).isInstanceOf(IllegalArgumentException.class);
        }
    }

    private InvocationLogEngine engine() {
        SlowLogHandler probe = new SlowLogHandler() {
            @Override
            public String render(InvocationLogEvent event, InvocationLogPolicy policy, LogValueFormatter formatter) {
                return policy.getLevel() + ":" + policy.getThreshold();
            }
        };
        return new InvocationLogEngine(properties, String::valueOf, records::add,
                Collections.singletonList(probe), Collections.emptyList());
    }

    private LogInvocation call(Class<?> type, String name, InvocationLogSource source) throws Exception {
        Method method = type.getMethod(name);
        return LogInvocation.builder().method(method).targetClass(type).source(source).build();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @CostLog
    @SlowLog(threshold = 200)
    @interface TimingPreset {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @SlowLog
    @interface AliasedPreset {
        @AliasFor(annotation = SlowLog.class, attribute = "threshold")
        long threshold() default 250;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface NoFunctions {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @CostLog
    @interface Required {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @SlowLog(threshold = -1)
    @interface InvalidThreshold {
    }

    @EntryArgs
    @SlowLog(level = LogPolicyLevel.OFF, threshold = 2000)
    static class Layered {
        @SlowLog(level = LogPolicyLevel.DEBUG)
        public String call() { return "ok"; }
    }

    static class Plain {
        public String call() { return "ok"; }
    }

    static class Explicit {
        @SlowLog(level = LogPolicyLevel.DEBUG)
        public String call() { return "ok"; }
    }

    static class Single {
        @CostLog
        public String call() { return "ok"; }
    }
}
