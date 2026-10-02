package io.github.luminion.velo.log.trace;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class VeloTraceEnvironmentPostProcessorTest {

  @Test
  void shouldAddTraceIdToDefaultLevelPattern() {
    MockEnvironment environment = new MockEnvironment();

    new VeloTraceEnvironmentPostProcessor()
        .postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty("logging.pattern.level")).isEqualTo("%5p [%X{traceId}]");
  }

  @Test
  void shouldUseConfiguredMdcKey() {
    MockEnvironment environment =
        new MockEnvironment().withProperty("velo.log.trace.mdc-key", "tid");

    new VeloTraceEnvironmentPostProcessor()
        .postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty("logging.pattern.level")).isEqualTo("%5p [%X{tid}]");
  }

  @Test
  void shouldNotInjectAnyDateFormatProperty() {
    MockEnvironment environment = new MockEnvironment();

    new VeloTraceEnvironmentPostProcessor()
        .postProcessEnvironment(environment, new SpringApplication());

    // 日期默认值由独立处理器提供，traceId 增强不应改变日期配置。
    assertThat(environment.getProperty("logging.pattern.dateformat")).isNull();
    assertThat(environment.getProperty("logging.pattern.date-format")).isNull();
  }

  @Test
  void shouldNotOverrideExistingLevelPattern() {
    MockEnvironment environment =
        new MockEnvironment().withProperty("logging.pattern.level", "%5p");

    new VeloTraceEnvironmentPostProcessor()
        .postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty("logging.pattern.level")).isEqualTo("%5p");
  }

  @Test
  void shouldSkipWhenTraceDisabled() {
    MockEnvironment environment =
        new MockEnvironment().withProperty("velo.log.trace.enabled", "false");

    new VeloTraceEnvironmentPostProcessor()
        .postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty("logging.pattern.level")).isNull();
  }

  @Test
  void shouldSkipWhenLogDisabled() {
    MockEnvironment environment = new MockEnvironment().withProperty("velo.log.enabled", "false");

    new VeloTraceEnvironmentPostProcessor()
        .postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty("logging.pattern.level")).isNull();
  }
}
