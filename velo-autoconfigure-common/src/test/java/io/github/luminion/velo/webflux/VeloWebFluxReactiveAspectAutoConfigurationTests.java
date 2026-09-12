package io.github.luminion.velo.webflux;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.idempotent.VeloIdempotentAutoConfiguration;
import io.github.luminion.velo.idempotent.config.VeloIdempotentJdkAutoConfiguration;
import io.github.luminion.velo.lock.VeloLockAutoConfiguration;
import io.github.luminion.velo.lock.config.VeloLockJdkAutoConfiguration;
import io.github.luminion.velo.log.VeloLogAutoConfiguration;
import io.github.luminion.velo.ratelimit.VeloRateLimitAutoConfiguration;
import io.github.luminion.velo.ratelimit.config.VeloRateLimitJdkAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class VeloWebFluxReactiveAspectAutoConfigurationTests {

    private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    VeloCoreAutoConfiguration.class,
                    VeloIdempotentJdkAutoConfiguration.class,
                    VeloIdempotentAutoConfiguration.class,
                    VeloLockJdkAutoConfiguration.class,
                    VeloLockAutoConfiguration.class,
                    VeloRateLimitJdkAutoConfiguration.class,
                    VeloRateLimitAutoConfiguration.class,
                    VeloLogAutoConfiguration.class,
                    VeloWebFluxReactiveAspectAutoConfiguration.class))
            .withPropertyValues(
                    "velo.idempotent.backend=jdk",
                    "velo.lock.backend=jdk",
                    "velo.rate-limit.backend=jdk");

    @Test
    void shouldCreateReactiveConcurrencyAndMethodLogAspectsInWebFlux() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(WebFluxIdempotentAspect.class);
            assertThat(context).hasSingleBean(WebFluxLockAspect.class);
            assertThat(context).hasSingleBean(WebFluxRateLimitAspect.class);
            assertThat(context).hasSingleBean(WebFluxInvokeLogAspect.class);
            assertThat(context).hasSingleBean(WebFluxSlowLogAspect.class);
        });
    }

    @Test
    void shouldKeepReactiveAspectsWhenWebEnhancementIsDisabled() {
        contextRunner.withConfiguration(AutoConfigurations.of(VeloWebFluxAutoConfiguration.class))
                .withPropertyValues("velo.web.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(VeloWebFluxConfigurer.class);
                    assertThat(context).hasSingleBean(WebFluxIdempotentAspect.class);
                    assertThat(context).hasSingleBean(WebFluxLockAspect.class);
                    assertThat(context).hasSingleBean(WebFluxRateLimitAspect.class);
                    assertThat(context).hasSingleBean(WebFluxInvokeLogAspect.class);
                    assertThat(context).hasSingleBean(WebFluxSlowLogAspect.class);
                });
    }

    @Test
    void shouldNotCreateReactiveAspectsInNonReactiveApplication() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        VeloCoreAutoConfiguration.class,
                        VeloWebFluxReactiveAspectAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(WebFluxIdempotentAspect.class));
    }
}
