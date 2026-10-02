package io.github.luminion.velo.log;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.log.annotation.InvokeLog;
import io.github.luminion.velo.log.annotation.LogIgnore;
import io.github.luminion.velo.log.aspect.ScheduledLogAspect;
import io.github.luminion.velo.log.aspect.XxlJobLogAspect;
import io.github.luminion.velo.log.trace.VeloTraceAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.Scheduled;

class VeloSourceLogAutoConfigurationTests {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  VeloCoreAutoConfiguration.class,
                  VeloTraceAutoConfiguration.class,
                  VeloLogAutoConfiguration.class,
                  VeloSourceLogAutoConfiguration.class));

  @Test
  void enablesScheduledByDefaultAndSkipsAbsentXxlJobLibrary() {
    runner
        .withClassLoader(new FilteredClassLoader("com.xxl.job"))
        .run(
            context -> {
              assertThat(context).hasSingleBean(ScheduledLogAspect.class);
              assertThat(context).doesNotHaveBean(XxlJobLogAspect.class);
            });
  }

  @Test
  void sourceSwitchDisablesScheduledAdapter() {
    runner
        .withPropertyValues("velo.log.sources.scheduled.enabled=false", "velo.trace.enabled=false")
        .run(context -> assertThat(context).doesNotHaveBean(ScheduledLogAspect.class));
  }

  @Test
  void globalSwitchDisablesTaskAdapters() {
    runner
        .withPropertyValues("velo.log.enabled=false", "velo.trace.enabled=false")
        .run(
            context -> {
              assertThat(context).doesNotHaveBean(ScheduledLogAspect.class);
              assertThat(context).doesNotHaveBean(XxlJobLogAspect.class);
            });
  }

  @Test
  void loggingDisabledAndIgnoredMethodsStillReuseAndRestoreTrace() {
    runner
        .withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class))
        .withPropertyValues("velo.log.enabled=false")
        .withBean(ContextCalls.class, ContextCalls::new)
        .run(
            context -> {
              assertThat(context).doesNotHaveBean(InvocationLogEngine.class);
              ContextCalls calls = context.getBean(ContextCalls.class);
              MDC.put("traceId", "caller");
              try {
                String id =
                    calls.invoke(
                        () -> {
                          String outer = MDC.get("traceId");
                          assertThat(calls.invoke(() -> MDC.get("traceId"))).isEqualTo(outer);
                          return outer;
                        });
                assertThat(id).matches("[0-9a-f]{32}");
                assertThat(MDC.get("traceId")).isEqualTo("caller");
                String first = calls.scheduled();
                String second = calls.scheduled();
                assertThat(first).matches("[0-9a-f]{32}").isNotEqualTo(second);
                assertThat(MDC.get("traceId")).isEqualTo("caller");
              } finally {
                MDC.clear();
              }
            });
  }

  static class ContextCalls {
    @InvokeLog
    @LogIgnore
    public String invoke(java.util.function.Supplier<String> callback) {
      return callback.get();
    }

    @Scheduled(fixedDelay = 1000)
    public String scheduled() {
      return MDC.get("traceId");
    }
  }
}
