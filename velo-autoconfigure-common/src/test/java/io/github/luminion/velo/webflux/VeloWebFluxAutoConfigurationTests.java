package io.github.luminion.velo.webflux;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.web.filter.reactive.ServerWebExchangeContextFilter;

import static org.assertj.core.api.Assertions.assertThat;

class VeloWebFluxAutoConfigurationTests {

    private final ReactiveWebApplicationContextRunner webContextRunner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VeloCoreAutoConfiguration.class,
                    VeloWebFluxAutoConfiguration.class));

    @Test
    void shouldCreateWebFluxBeansByDefault() {
        webContextRunner.run(context -> {
            assertThat(context).hasSingleBean(VeloWebFluxConfigurer.class);
            assertThat(context).hasSingleBean(ServerWebExchangeContextFilter.class);
            assertThat(context).hasSingleBean(TraceIdWebFluxFilter.class);
            assertThat(context).hasSingleBean(WebFluxControllerLogAspect.class);
            assertThat(context).hasSingleBean(RuntimeJsonSerializer.class);
        });
    }

    @Test
    void shouldKeepUserRuntimeJsonSerializer() {
        RuntimeJsonSerializer serializer = value -> "custom";

        webContextRunner.withBean(RuntimeJsonSerializer.class, () -> serializer)
                .run(context -> assertThat(context.getBean(RuntimeJsonSerializer.class)).isSameAs(serializer));
    }

    @Test
    void shouldDisableWebFluxBeansWhenWebIsDisabled() {
        webContextRunner.withPropertyValues("velo.web.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(VeloWebFluxConfigurer.class);
                    assertThat(context).doesNotHaveBean(TraceIdWebFluxFilter.class);
                    assertThat(context).doesNotHaveBean(WebFluxControllerLogAspect.class);
                });
    }
}
