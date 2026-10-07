package io.github.luminion.velo.core;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.idempotent.VeloIdempotentAutoConfiguration;
import io.github.luminion.velo.idempotent.annotation.Idempotent;
import io.github.luminion.velo.lock.VeloLockAutoConfiguration;
import io.github.luminion.velo.lock.annotation.Lock;
import io.github.luminion.velo.ratelimit.VeloRateLimitAutoConfiguration;
import io.github.luminion.velo.ratelimit.annotation.RateLimit;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.spi.fingerprint.SpelFingerprinter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MissingConcurrencyBackendTests {

    @Test
    void unavailableBackendsFailBeforeExecutingAnnotatedBusinessMethods() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(VeloIdempotentAutoConfiguration.class,
                        VeloLockAutoConfiguration.class, VeloRateLimitAutoConfiguration.class))
                .withUserConfiguration(TestConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    GuardedService service = context.getBean(GuardedService.class);
                    assertThatThrownBy(() -> service.submit(1L)).isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("No IdempotentHandler");
                    assertThatThrownBy(() -> service.pay(1L)).isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("No LockHandler");
                    assertThatThrownBy(service::query).isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("No RateLimitHandler");
                    assertThat(service.getInvocations()).isZero();
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    @EnableConfigurationProperties(VeloProperties.class)
    static class TestConfiguration {
        @Bean
        Fingerprinter fingerprinter() {
            return new SpelFingerprinter();
        }

        @Bean
        GuardedService guardedService() {
            return new GuardedService();
        }
    }

    public static class GuardedService {
        private int invocations;

        @Idempotent("#p0")
        public void submit(Long id) {
            invocations++;
        }

        @Lock("#p0")
        public void pay(Long id) {
            invocations++;
        }

        @RateLimit(qps = 1)
        public void query() {
            invocations++;
        }

        public int getInvocations() {
            return invocations;
        }
    }
}
