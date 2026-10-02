package io.github.luminion.velo.log.trace;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luminion.velo.VeloProperties.TraceProperties;
import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.log.InvocationLogEngine;
import io.github.luminion.velo.log.VeloLogAutoConfiguration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.task.TaskDecorator;

@ExtendWith(OutputCaptureExtension.class)
class VeloTraceAutoConfigurationTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(VeloTraceAutoConfiguration.class));

  @Test
  void worksWithoutLoggingAutoConfigurationOrAspectJ() {
    runner
        .withClassLoader(new FilteredClassLoader("org.aspectj"))
        .withPropertyValues("velo.log.enabled=false")
        .run(
            context ->
                assertThat(context)
                    .hasSingleBean(TraceContextResolver.class)
                    .hasSingleBean(TraceScopeManager.class)
                    .hasSingleBean(TaskDecorator.class)
                    .doesNotHaveBean(InvocationLogEngine.class));
  }

  @Test
  void newConfigurationOverridesLegacyPerProperty() {
    runner
        .withPropertyValues(
            "velo.log.trace.enabled=false",
            "velo.log.trace.mdc-key=legacyId",
            "velo.log.trace.feign-propagation-enabled=false",
            "velo.trace.enabled=true",
            "velo.trace.mdc-key=currentId")
        .run(
            context -> {
              assertThat(context).hasSingleBean(TraceScopeManager.class);
              TraceProperties properties = context.getBean(TraceProperties.class);
              assertThat(properties.isEnabled()).isTrue();
              assertThat(properties.getMdcKey()).isEqualTo("currentId");
              assertThat(properties.isFeignPropagationEnabled()).isFalse();
            });
  }

  @Test
  void legacyConfigurationStillDisablesTrace() {
    runner
        .withPropertyValues("velo.log.trace.enabled=false")
        .run(
            context ->
                assertThat(context)
                    .doesNotHaveBean(TraceContextResolver.class)
                    .doesNotHaveBean(TraceScopeManager.class)
                    .doesNotHaveBean(TaskDecorator.class));
  }

  @Test
  void disablingTraceKeepsLoggingEngine() {
    runner
        .withConfiguration(
            AutoConfigurations.of(VeloCoreAutoConfiguration.class, VeloLogAutoConfiguration.class))
        .withPropertyValues("velo.trace.enabled=false")
        .run(
            context ->
                assertThat(context)
                    .hasSingleBean(InvocationLogEngine.class)
                    .doesNotHaveBean(TraceScopeManager.class));
  }

  @Test
  void environmentVariablesUseTheSameBindingForConditionsAndProperties() {
    Map<String, Object> variables = new LinkedHashMap<>();
    variables.put("VELO_TRACE_ENABLED", "true");
    variables.put("VELO_TRACE_MDCKEY", "environmentId");
    variables.put("VELO_TRACE_FEIGNPROPAGATIONENABLED", "false");
    runner
        .withPropertyValues("velo.log.trace.enabled=false")
        .withInitializer(
            context ->
                context
                    .getEnvironment()
                    .getPropertySources()
                    .addFirst(new SystemEnvironmentPropertySource("systemEnvironment", variables)))
        .run(
            context -> {
              assertThat(context).hasSingleBean(TraceScopeManager.class);
              TraceProperties properties = context.getBean(TraceProperties.class);
              assertThat(properties.getMdcKey()).isEqualTo("environmentId");
              assertThat(properties.isFeignPropagationEnabled()).isFalse();
              assertThat(
                      TraceEnabledCondition.property(
                          context.getEnvironment(),
                          "feign-propagation-enabled",
                          Boolean.class,
                          true))
                  .isFalse();
            });
  }

  @Test
  void warnsForInvalidIndependentMdcKeyWhenLoggingIsDisabled(CapturedOutput output) {
    runner
        .withPropertyValues("velo.log.enabled=false", "velo.trace.mdc-key=")
        .run(context -> assertThat(context).hasSingleBean(TraceScopeManager.class));
    assertThat(output.getOut()).contains("velo.trace.mdc-key 为空");
  }
}
