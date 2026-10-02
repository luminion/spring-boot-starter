package io.github.luminion.velo.log;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.log.aspect.InvokeLogAspect;
import io.github.luminion.velo.log.support.Slf4JInvocationLogWriter;
import io.github.luminion.velo.log.trace.TraceContext;
import io.github.luminion.velo.log.trace.TraceContextResolver;
import io.github.luminion.velo.log.trace.TraceData;
import io.github.luminion.velo.log.trace.W3cTraceContextResolver;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.TaskDecorator;

class VeloLogAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  VeloCoreAutoConfiguration.class, VeloLogAutoConfiguration.class));

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
  void customResolverReplacesDefaultAndIsUsedByEngineAndAsyncDecorator() {
    TraceData data =
        new TraceData("custom-bean", Collections.singletonMap("X-Custom", "custom-bean"));
    TraceContextResolver resolver = () -> data;
    contextRunner
        .withBean(TraceContextResolver.class, () -> resolver)
        .run(
            context -> {
              assertThat(context).hasSingleBean(TraceContextResolver.class);
              assertThat(context.getBean(TraceContextResolver.class)).isSameAs(resolver);
              try (TraceContext.Scope scope =
                  context.getBean(InvocationLogEngine.class).openRootTrace()) {
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
              assertThat(context).doesNotHaveBean(InvokeLogAspect.class);
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
        context -> assertThat(context.getBean(LogValueFormatter.class).format(42)).isEqualTo("42"));
  }

  @Test
  void bindsIndependentLevelsAndZeroThreshold() {
    contextRunner
        .withPropertyValues(
            "velo.log.sources.controller.entry-args.level=WARN",
            "velo.log.sources.invoke.exit-result.enabled=false",
            "velo.log.defaults.slow-log.threshold-ms=0")
        .run(
            context -> {
              assertThat(context).hasSingleBean(InvocationLogEngine.class);
              VeloProperties properties = context.getBean(VeloProperties.class);
              assertThat(properties.getLog().getSources().getController().getEntryArgs().getLevel())
                  .isEqualTo(LogLevel.WARN);
              assertThat(properties.getLog().getSources().getInvoke().getExitResult().getEnabled())
                  .isFalse();
              assertThat(properties.getLog().getDefaults().getSlowLog().getThresholdMs()).isZero();
            });
  }

  @Test
  void rejectsNegativeSlowThresholdAtStartup() {
    contextRunner
        .withPropertyValues("velo.log.sources.feign.slow-log.threshold-ms=-1")
        .run(context -> assertThat(context).hasFailed());
  }

  static final class CustomInvocationLogWriter implements InvocationLogWriter {

    @Override
    public void write(InvocationLogRecord record) {}
  }
}
