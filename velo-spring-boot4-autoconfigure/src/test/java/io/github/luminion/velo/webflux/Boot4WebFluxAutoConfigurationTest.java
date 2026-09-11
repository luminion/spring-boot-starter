package io.github.luminion.velo.webflux;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.http.codec.ServerCodecConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

class Boot4WebFluxAutoConfigurationTest {

    @Test
    void shouldLoadWebFluxEnhancementsOnSpringBoot4() {
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(VeloCoreAutoConfiguration.class,
                        VeloWebFluxAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(VeloWebFluxConfigurer.class);
                    assertThat(context).hasSingleBean(TraceIdWebFluxFilter.class);
                    assertThat(context).hasSingleBean(WebFluxControllerLogAspect.class);
        });
    }

    @Test
    void shouldSerializeWithBoot4WebFluxCodec() {
        RuntimeJsonSerializer serializer = new WebFluxRuntimeJsonSerializer(
                ServerCodecConfigurer.create().getWriters());

        assertThat(serializer.toJson(new DemoPayload("boot4"))).contains("\"value\":\"boot4\"");
    }

    static class DemoPayload {

        private final String value;

        DemoPayload(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
