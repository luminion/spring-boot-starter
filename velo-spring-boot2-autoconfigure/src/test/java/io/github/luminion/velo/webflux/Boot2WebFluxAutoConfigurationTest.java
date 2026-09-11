package io.github.luminion.velo.webflux;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class Boot2WebFluxAutoConfigurationTest {

    @Test
    void shouldLoadWebFluxEnhancementsOnSpringBoot2() {
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(VeloCoreAutoConfiguration.class,
                        VeloWebFluxAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(VeloWebFluxConfigurer.class);
                    assertThat(context).hasSingleBean(TraceIdWebFluxFilter.class);
                    assertThat(context).hasSingleBean(WebFluxControllerLogAspect.class);
                });
    }
}
