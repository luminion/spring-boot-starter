package io.github.luminion.velo.log.config;



import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.mock.env.MockEnvironment;

class VeloLogEnvironmentPostProcessorTest {
    private final VeloLogEnvironmentPostProcessor processor = new VeloLogEnvironmentPostProcessor();

    @Test
    void providesDefaultBeforeLoggingInitialization() {
        MockEnvironment environment = new MockEnvironment();

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("logging.pattern.dateformat"))
                .isEqualTo("yyyy-MM-dd HH:mm:ss.SSS");
        assertThat(environment.getPropertySources().get("veloLogPatternDefaults"))
                .isSameAs(environment.getPropertySources().stream().reduce((first, last) -> last).get());
        assertThat(processor.getOrder()).isGreaterThan(ConfigDataEnvironmentPostProcessor.ORDER);
        assertThat(
                SpringFactoriesLoader.loadFactoryNames(
                        EnvironmentPostProcessor.class, getClass().getClassLoader()))
                .contains(VeloLogEnvironmentPostProcessor.class.getName());
    }

    @Test
    void preservesConfiguredDateFormat() {
        MockEnvironment environment =
                new MockEnvironment().withProperty("logging.pattern.dateformat", "HH:mm:ss");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("logging.pattern.dateformat")).isEqualTo("HH:mm:ss");
        assertThat(environment.getPropertySources().contains("veloLogPatternDefaults")).isFalse();
    }

    @Test
    void preservesDateFormatFromEnvironmentVariable() {
        MockEnvironment environment = new MockEnvironment();
        environment
                .getPropertySources()
                .addFirst(
                        new SystemEnvironmentPropertySource(
                                "externalEnvironment",
                                Collections.singletonMap("LOGGING_PATTERN_DATEFORMAT", "HH:mm:ss")));

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("logging.pattern.dateformat")).isEqualTo("HH:mm:ss");
        assertThat(environment.getPropertySources().contains("veloLogPatternDefaults")).isFalse();
    }

    @Test
    void preservesExplicitEmptyDateFormat() {
        MockEnvironment environment =
                new MockEnvironment().withProperty("logging.pattern.dateformat", "");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("logging.pattern.dateformat")).isEmpty();
    }

    @Test
    void laterUserConfigurationOverridesDefault() {
        MockEnvironment environment = new MockEnvironment();
        processor.postProcessEnvironment(environment, new SpringApplication());

        environment
                .getPropertySources()
                .addFirst(
                        new MapPropertySource(
                                "userConfiguration",
                                Collections.singletonMap("logging.pattern.dateformat", "HH:mm:ss")));

        assertThat(environment.getProperty("logging.pattern.dateformat")).isEqualTo("HH:mm:ss");
    }

    @ParameterizedTest
    @ValueSource(strings = {"velo.opinionated", "velo.log.enabled"})
    void skipsWhenGlobalEnhancementsDisabled(String property) {
        MockEnvironment environment = new MockEnvironment().withProperty(property, "false");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("logging.pattern.dateformat")).isNull();
    }

    @Test
    void dateFormatDoesNotDependOnTraceSwitch() {
        MockEnvironment environment =
                new MockEnvironment().withProperty("velo.log.trace.enabled", "false");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("logging.pattern.dateformat"))
                .isEqualTo("yyyy-MM-dd HH:mm:ss.SSS");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                    "LOG_DATEFORMAT_PATTERN",
                    "logging.config",
                    "logback.configurationFile",
                    "log4j.configurationFile",
                    "log4j2.configurationFile",
                    "logging.pattern.console",
                    "logging.pattern.file",
                    "CONSOLE_LOG_PATTERN",
                    "FILE_LOG_PATTERN",
                    "logging.structured.format.console",
                    "logging.structured.format.file",
                    "CONSOLE_LOG_STRUCTURED_FORMAT",
                    "FILE_LOG_STRUCTURED_FORMAT"
            })
    void preservesCustomLoggingProperties(String property) {
        MockEnvironment environment = new MockEnvironment().withProperty(property, "custom");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("logging.pattern.dateformat")).isNull();
        assertThat(environment.getProperty(property)).isEqualTo("custom");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"logback-spring.xml", "log4j2.xml", "log4j2-spring.yml", "logging.properties"})
    void preservesClasspathLoggingConfiguration(String configuration) {
        ResourceLoader resources = mock(ResourceLoader.class);
        Resource missing = mock(Resource.class);
        Resource custom = mock(Resource.class);
        when(custom.exists()).thenReturn(true);
        when(resources.getResource(anyString())).thenReturn(missing);
        when(resources.getResource("classpath:" + configuration)).thenReturn(custom);
        SpringApplication application = new SpringApplication();
        application.setResourceLoader(resources);
        MockEnvironment environment = new MockEnvironment();

        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getProperty("logging.pattern.dateformat")).isNull();
    }
}
