package io.github.luminion.velo.log.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.log.annotation.CostLog;
import io.github.luminion.velo.log.annotation.EntryArgs;
import io.github.luminion.velo.log.annotation.SlowLog;
import io.github.luminion.velo.log.aspect.InvokeLogAspect;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.handler.CostLogHandler;
import io.github.luminion.velo.log.model.InvocationLogContext;
import io.github.luminion.velo.log.model.InvocationLogEvent;
import io.github.luminion.velo.log.model.InvocationLogPolicy;
import io.github.luminion.velo.log.model.InvocationLogRecord;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.model.LogInvocation;
import io.github.luminion.velo.log.spi.InvocationLogContextFormatter;
import io.github.luminion.velo.log.spi.InvocationLogHandler;
import io.github.luminion.velo.log.spi.InvocationLogWriter;
import io.github.luminion.velo.log.spi.LogValueFormatter;
import io.github.luminion.velo.log.support.InvocationLogSupport;
import io.github.luminion.velo.log.writer.Slf4JInvocationLogWriter;
import io.github.luminion.velo.trace.TraceContext;
import io.github.luminion.velo.trace.TraceContextResolver;
import io.github.luminion.velo.trace.TraceData;
import io.github.luminion.velo.trace.TraceScopeManager;
import io.github.luminion.velo.trace.VeloTraceAutoConfiguration;
import io.github.luminion.velo.trace.W3cTraceContextResolver;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.TaskDecorator;
import org.springframework.util.ReflectionUtils;

/**
 * 验证调用日志自动配置、属性绑定及自定义扩展点。
 */
class VeloLogAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    VeloCoreAutoConfiguration.class,
                                    VeloTraceAutoConfiguration.class,
                                    VeloLogAutoConfiguration.class,
                                    VeloSourceLogAutoConfiguration.class));

    @Test
    void shouldCreateUnifiedInvocationLoggingBeansByDefault() {
        contextRunner.run(
                context -> {
                    assertThat(context).hasSingleBean(InvocationLogWriter.class);
                    assertThat(context.getBean(InvocationLogWriter.class))
                            .isInstanceOf(Slf4JInvocationLogWriter.class);
                    assertThat(context).hasSingleBean(InvokeLogAspect.class);
                    assertThat(context.getBean(TraceContextResolver.class))
                            .isInstanceOf(W3cTraceContextResolver.class);
                });
    }

    @Test
    void customResolverReplacesDefaultAndIsUsedByScopeAndAsyncDecorator() {
        TraceData data =
                new TraceData("custom-bean", Collections.singletonMap("X-Custom", "custom-bean"));
        TraceContextResolver resolver = () -> data;
        contextRunner
                .withBean(TraceContextResolver.class, () -> resolver)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(TraceContextResolver.class);
                            assertThat(context.getBean(TraceContextResolver.class)).isSameAs(resolver);
                            try (TraceContext.Scope scope = context.getBean(TraceScopeManager.class).root()) {
                                assertThat(TraceContext.current()).isSameAs(data);
                            }
                            context
                                    .getBean(TaskDecorator.class)
                                    .decorate(() -> assertThat(TraceContext.current()).isSameAs(data))
                                    .run();
                            assertThat(TraceContext.current()).isNull();
                        });
    }

    @Test
    void shouldPreferCustomInvocationLogWriterWhenAvailable() {
        contextRunner
                .withBean(CustomInvocationLogWriter.class, CustomInvocationLogWriter::new)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(InvocationLogWriter.class);
                            assertThat(context.getBean(InvocationLogWriter.class))
                                    .isInstanceOf(CustomInvocationLogWriter.class);
                            assertThat(context).hasSingleBean(InvokeLogAspect.class);
                        });
    }

    @Test
    void shouldKeepAnnotationLoggingIndependentFromEndpointLogging() {
        contextRunner
                .withPropertyValues(
                        "velo.log.sources.controller.enabled=false", "velo.log.sources.feign.enabled=false")
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(InvocationLogWriter.class);
                            assertThat(context).hasSingleBean(InvokeLogAspect.class);
                        });
    }

    @Test
    void shouldSkipAllLoggingBeansWhenLogDisabled() {
        contextRunner
                .withPropertyValues("velo.log.enabled=false")
                .run(
                        context -> {
                            assertThat(context).doesNotHaveBean(InvocationLogWriter.class);
                            assertThat(context).hasSingleBean(InvokeLogAspect.class);
                            assertThat(context).hasSingleBean(TraceScopeManager.class);
                        });
    }

    @Test
    void usesCustomFormatterAndDecoratorWithoutReplacement() {
        LogValueFormatter formatter = value -> "custom";
        TaskDecorator decorator = task -> task;
        contextRunner
                .withBean(LogValueFormatter.class, () -> formatter)
                .withBean(TaskDecorator.class, () -> decorator)
                .run(
                        context -> {
                            assertThat(context.getBean(LogValueFormatter.class)).isSameAs(formatter);
                            assertThat(context.getBean(TaskDecorator.class)).isSameAs(decorator);
                        });
    }

    @Test
    void noMapperUsesToStringFallback() {
        contextRunner.run(
                context -> assertThat(InvocationLogSupport.format(42, context.getBean(LogValueFormatter.class))).isEqualTo("42"));
    }

    @Test
    void springBeansReplaceOneHandlerAndOnePublicBlock() {
        List<InvocationLogRecord> records = new ArrayList<>();
        InvocationLogHandler handler =
                new CostLogHandler() {
                    @Override
                    public String render(InvocationLogEvent event,
                            InvocationLogPolicy policy, LogValueFormatter formatter) {
                        return "custom-cost";
                    }
                };
        InvocationLogContextFormatter block =
                new InvocationLogContextFormatter() {
                    @Override
                    public InvocationLogSource getSource() {
                        return InvocationLogSource.CONTROLLER;
                    }
                    @Override
                    public String format(LogInvocation invocation,
                            InvocationLogContext context) {
                        return "tenant=demo";
                    }
                };
        contextRunner.withBean(InvocationLogWriter.class, () -> records::add)
                .withBean(InvocationLogHandler.class, () -> handler)
                .withBean(InvocationLogContextFormatter.class, () -> block)
                .withPropertyValues("velo.log.sources.controller.default-annotation="
                        + CostLog.class.getName())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    Method method = ReflectionUtils.findMethod(EngineService.class, "call");
                    LogInvocation invocation =
                            LogInvocation.builder().method(method)
                                    .targetClass(EngineService.class)
                                    .source(InvocationLogSource.CONTROLLER).build();
                    InvocationLogEngine engine = context.getBean(InvocationLogEngine.class);
                    Assertions.assertThatCode(() -> engine.invoke(invocation, () -> "ok"))
                            .doesNotThrowAnyException();
                    assertThat(records).singleElement().satisfies(record -> {
                        assertThat(record.getContent()).isEqualTo("custom-cost");
                        assertThat(record.getContextContent()).isEqualTo("tenant=demo");
                    });
                });
    }

    public static class EngineService {
        public String call() {
            return "ok";
        }
    }

    @Test
    void bindsTheDefaultCombinationType() {
        contextRunner.withPropertyValues("velo.log.sources.controller.default-annotation=" + TimingPreset.class.getName())
                .run(context -> {
                    assertThat(context).hasSingleBean(InvocationLogEngine.class);
                    assertThat(context.getBean(VeloProperties.class).getLog().getSources().getController()
                            .getDefaultAnnotation()).isEqualTo(TimingPreset.class);
                });
    }

    @Test
    void rejectsInvalidThresholdAndLengthInTheSelectedCombination() {
        for (Class<?> type : new Class<?>[]{InvalidThreshold.class, InvalidLength.class}) {
            contextRunner.withPropertyValues("velo.log.sources.feign.default-annotation=" + type.getName())
                    .run(context -> assertThat(context).hasFailed());
        }
    }

    @Test
    void acceptsACombinationThatDisablesPayload() {
        contextRunner.withPropertyValues("velo.log.sources.controller.default-annotation=" + NoPayload.class.getName())
                .run(context -> assertThat(context).hasSingleBean(InvocationLogEngine.class));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @CostLog
    @SlowLog(threshold = 0)
    @interface TimingPreset {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @SlowLog(threshold = -1)
    @interface InvalidThreshold {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @EntryArgs(maxLength = -2)
    @interface InvalidLength {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @EntryArgs(maxLength = 0)
    @interface NoPayload {
    }

    static final class CustomInvocationLogWriter implements InvocationLogWriter {

        @Override
        public void write(InvocationLogRecord record) {
        }
    }
}
