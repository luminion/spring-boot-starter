package io.github.luminion.velo.core;

import org.apache.commons.logging.impl.NoOpLog;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class VeloModeEnvironmentPostProcessorTest {

    private static final DeferredLogFactory LOG_FACTORY = supplier -> new NoOpLog();

    @Test
    void shouldNotApplyNonInvasiveDefaultsByDefault() {
        MockEnvironment environment = new MockEnvironment();

        new VeloModeEnvironmentPostProcessor(LOG_FACTORY).postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("velo.log.trace.enabled")).isNull();
        assertThat(environment.getProperty("velo.jackson.enabled")).isNull();
    }

    @Test
    void shouldApplyNonInvasiveDefaultsWhenDisabled() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("velo.opinionated", "false");

        new VeloModeEnvironmentPostProcessor(LOG_FACTORY).postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("velo.log.trace.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("velo.log.controller.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("velo.log.feign.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("velo.jackson.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("velo.spring-converter.date-time-enabled")).isEqualTo("false");
        assertThat(environment.getProperty("velo.mybatis-plus.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("velo.cache.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("velo.redis.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("velo.excel.converters.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("velo.idempotent.enabled")).isNull();
        assertThat(environment.getProperty("velo.rate-limit.enabled")).isNull();
        assertThat(environment.getProperty("velo.lock.enabled")).isNull();
        assertThat(environment.getProperty("velo.web.cors.enabled")).isNull();
        assertThat(environment.getProperty("velo.xss.strategy")).isNull();
    }

    @Test
    void shouldLetExplicitPropertiesOverrideNonInvasiveDefaults() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("velo.opinionated", "false")
                .withProperty("velo.log.trace.enabled", "true")
                .withProperty("velo.log.controller.enabled", "true")
                .withProperty("velo.log.feign.enabled", "true")
                .withProperty("velo.jackson.enabled", "true")
                .withProperty("velo.spring-converter.date-time-enabled", "true")
                .withProperty("velo.mybatis-plus.enabled", "true")
                .withProperty("velo.cache.enabled", "true")
                .withProperty("velo.redis.enabled", "true")
                .withProperty("velo.excel.converters.enabled", "true");

        new VeloModeEnvironmentPostProcessor(LOG_FACTORY).postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("velo.log.trace.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("velo.log.controller.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("velo.log.feign.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("velo.jackson.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("velo.spring-converter.date-time-enabled")).isEqualTo("true");
        assertThat(environment.getProperty("velo.mybatis-plus.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("velo.cache.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("velo.redis.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("velo.excel.converters.enabled")).isEqualTo("true");
    }

    @Test
    void shouldRunAfterSpringBootConfigDataEnvironmentPostProcessor() {
        assertThat(new VeloModeEnvironmentPostProcessor(LOG_FACTORY).getOrder())
                .isEqualTo(ConfigDataEnvironmentPostProcessor.ORDER + 1);
    }

    @Test
    void shouldReadConfigDataPropertiesBeforeAddingLowestPriorityDefaults() {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "applicationConfig",
                Collections.<String, Object>singletonMap("velo.opinionated", "false")));

        new VeloModeEnvironmentPostProcessor(LOG_FACTORY).postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("velo.opinionated")).isEqualTo("false");
        assertThat(environment.getProperty("velo.jackson.enabled")).isEqualTo("false");
        assertThat(environment.getPropertySources().get("veloOpinionatedDefaults")).isNotNull();
    }
}
