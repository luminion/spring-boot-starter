package io.github.luminion.velo.core;

import org.apache.commons.logging.impl.NoOpLog;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class VeloModeEnvironmentPostProcessorTest {

    private static final DeferredLogFactory LOG_FACTORY = supplier -> new NoOpLog();

    @Test
    void shouldNotApplyConservativeDefaultsByDefault() {
        MockEnvironment environment = new MockEnvironment();

        new VeloModeEnvironmentPostProcessor(LOG_FACTORY).postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("velo.log.trace.enabled")).isNull();
        assertThat(environment.getProperty("velo.jackson.enabled")).isNull();
    }

    @Test
    void shouldApplyConservativeDefaultsWhenConfigured() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("velo.mode", "conservative");

        new VeloModeEnvironmentPostProcessor(LOG_FACTORY).postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("velo.log.trace.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("velo.log.invocation.controller.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("velo.log.invocation.feign.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("velo.jackson.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("velo.spring-converter.date-time-enabled")).isEqualTo("false");
        assertThat(environment.getProperty("velo.mybatis-plus.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("velo.cache.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("velo.redis.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("velo.excel.converters.enabled")).isEqualTo("false");
    }

    @Test
    void shouldLetExplicitPropertiesOverrideConservativeDefaults() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("velo.mode", "conservative")
                .withProperty("velo.log.trace.enabled", "true")
                .withProperty("velo.jackson.enabled", "true");

        new VeloModeEnvironmentPostProcessor(LOG_FACTORY).postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("velo.log.trace.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("velo.jackson.enabled")).isEqualTo("true");
    }
}
