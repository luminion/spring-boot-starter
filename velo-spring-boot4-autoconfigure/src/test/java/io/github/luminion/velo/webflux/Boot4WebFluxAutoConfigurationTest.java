package io.github.luminion.velo.webflux;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.idempotent.VeloIdempotentAutoConfiguration;
import io.github.luminion.velo.idempotent.config.VeloIdempotentJdkAutoConfiguration;
import io.github.luminion.velo.lock.VeloLockAutoConfiguration;
import io.github.luminion.velo.lock.config.VeloLockJdkAutoConfiguration;
import io.github.luminion.velo.log.VeloLogAutoConfiguration;
import io.github.luminion.velo.ratelimit.VeloRateLimitAutoConfiguration;
import io.github.luminion.velo.ratelimit.config.VeloRateLimitJdkAutoConfiguration;
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
                        VeloIdempotentJdkAutoConfiguration.class,
                        VeloIdempotentAutoConfiguration.class,
                        VeloLockJdkAutoConfiguration.class,
                        VeloLockAutoConfiguration.class,
                        VeloRateLimitJdkAutoConfiguration.class,
                        VeloRateLimitAutoConfiguration.class,
                        VeloLogAutoConfiguration.class,
                        VeloWebFluxAutoConfiguration.class,
                        VeloWebFluxReactiveAspectAutoConfiguration.class))
                .withPropertyValues("velo.idempotent.backend=jdk", "velo.lock.backend=jdk",
                        "velo.rate-limit.backend=jdk")
                .run(context -> {
                    assertThat(context).hasSingleBean(VeloWebFluxConfigurer.class);
                    assertThat(context).hasSingleBean(TraceIdWebFluxFilter.class);
                    assertThat(context).hasSingleBean(WebFluxControllerLogAspect.class);
                    assertThat(context).hasSingleBean(WebFluxIdempotentAspect.class);
                    assertThat(context).hasSingleBean(WebFluxLockAspect.class);
                    assertThat(context).hasSingleBean(WebFluxRateLimitAspect.class);
                    assertThat(context).hasSingleBean(WebFluxInvokeLogAspect.class);
                    assertThat(context).hasSingleBean(WebFluxSlowLogAspect.class);
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
