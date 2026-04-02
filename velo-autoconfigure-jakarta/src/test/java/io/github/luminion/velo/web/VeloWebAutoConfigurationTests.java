package io.github.luminion.velo.web;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class VeloWebAutoConfigurationTests {

    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    VeloCoreAutoConfiguration.class,
                    VeloWebAutoConfiguration.class
            ));

    @Test
    void shouldCreateWebMvcConfigurerByDefaultInServletApplication() {
        webContextRunner.run(context -> assertThat(context).hasSingleBean(VeloWebMvcConfigurer.class));
    }

    @Test
    void shouldCreateControllerLogAspectOnlyWhenRequestLoggingEnabled() {
        webContextRunner
                .withPropertyValues("velo.web.request-logging.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(ControllerLogAspect.class));
    }
}
