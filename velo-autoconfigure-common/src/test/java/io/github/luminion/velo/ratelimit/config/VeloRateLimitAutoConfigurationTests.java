package io.github.luminion.velo.ratelimit.config;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.ratelimit.VeloRateLimitAutoConfiguration;
import io.github.luminion.velo.ratelimit.aspect.RateLimitAspect;
import io.github.luminion.velo.ratelimit.support.GuavaRateLimitHandler;
import io.github.luminion.velo.ratelimit.support.RedisRateLimitHandler;
import io.github.luminion.velo.ratelimit.support.RedissonRateLimitHandler;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.spi.fingerprint.SpelFingerprinter;
import io.github.luminion.velo.test.TestStringRedisTemplate;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class VeloRateLimitAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = coreRunner()
            .withConfiguration(AutoConfigurations.of(
                    VeloRateLimitRedissonAutoConfiguration.class,
                    VeloRateLimitRedisConfiguration.class,
                    VeloRateLimitGuavaAutoConfiguration.class));

    private static ApplicationContextRunner coreRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(VeloRateLimitAutoConfiguration.class))
                .withUserConfiguration(PropertiesConfiguration.class)
                .withBean(Fingerprinter.class, SpelFingerprinter::new);
    }

    @Test
    void shouldCreateDefaultRateLimitHandlerAndAspect() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(RateLimitAspect.class);
            assertThat(context.getBean(RateLimitHandler.class)).isInstanceOf(GuavaRateLimitHandler.class);
        });
    }

    @Test
    void shouldPreferRedissonWhenRedisIsAlsoAvailable() {
        contextRunner.withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withBean(StringRedisTemplate.class, TestStringRedisTemplate::new)
                .run(context -> assertThat(context.getBean(RateLimitHandler.class))
                        .isInstanceOf(RedissonRateLimitHandler.class));
    }

    @Test
    void shouldPreferRedisToGuava() {
        contextRunner.withBean(StringRedisTemplate.class, TestStringRedisTemplate::new)
                .run(context -> assertThat(context.getBean(RateLimitHandler.class))
                        .isInstanceOf(RedisRateLimitHandler.class));
    }

    @Test
    void shouldRejectExplicitRemovedJdkBackend() {
        contextRunner.withPropertyValues("velo.rate-limit.backend=jdk")
                .withBean(StringRedisTemplate.class, TestStringRedisTemplate::new)
                .run(context -> assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
                        .hasStackTraceContaining("velo.rate-limit.backend=JDK")
                        .hasStackTraceContaining("Supported backends"));
    }

    @Test
    void shouldUseExplicitGuavaBackendEvenWhenRedisIsAvailable() {
        contextRunner.withPropertyValues("velo.rate-limit.backend=guava")
                .withBean(StringRedisTemplate.class, TestStringRedisTemplate::new)
                .run(context -> assertThat(context.getBean(RateLimitHandler.class))
                        .isInstanceOf(GuavaRateLimitHandler.class));
    }

    @Test
    void customHandlerShouldOverrideBackendSelection() {
        RateLimitHandler custom = (key, qps) -> true;
        contextRunner.withPropertyValues("velo.rate-limit.backend=redis")
                .withBean(RateLimitHandler.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(RateLimitHandler.class)).isSameAs(custom);
                });
    }

    @Test
    void shouldFailWhenExplicitRedissonDependencyIsMissing() {
        contextRunner.withPropertyValues("velo.rate-limit.backend=redisson")
                .run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }

    @Test
    void disabledFeatureShouldNotCreateHandlerOrAspect() {
        contextRunner.withPropertyValues("velo.rate-limit.enabled=false")
                .run(context -> assertThat(context).hasNotFailed()
                        .doesNotHaveBean(RateLimitHandler.class).doesNotHaveBean(RateLimitAspect.class));
    }

    @Test
    void missingHandlerShouldFailWhenUsed() {
        coreRunner().run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(RateLimitAspect.class);
            assertThatThrownBy(() -> context.getBean(RateLimitHandler.class).tryAcquire("test", 1))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("No RateLimitHandler");
        });
    }

    @Test
    void missingGuavaShouldUseUnavailableHandler() {
        contextRunner.withClassLoader(new FilteredClassLoader("com.google.common"))
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(RateLimitAspect.class);
                    assertThatThrownBy(() -> context.getBean(RateLimitHandler.class).tryAcquire("test", 1))
                            .isInstanceOf(IllegalStateException.class).hasMessageContaining("No RateLimitHandler");
                });
    }

    @Test
    void explicitGuavaWithoutDependencyShouldFailClearlyAtStartup() {
        contextRunner.withClassLoader(new FilteredClassLoader("com.google.common"))
                .withPropertyValues("velo.rate-limit.backend=guava")
                .run(context -> assertThat(context.getStartupFailure())
                        .hasRootCauseInstanceOf(IllegalStateException.class)
                        .hasStackTraceContaining("velo.rate-limit.backend=GUAVA"));
    }

    @Test
    void shouldUseExplicitRedisBackend() {
        contextRunner.withPropertyValues("velo.rate-limit.backend=redis")
                .withBean("businessRedisTemplate", StringRedisTemplate.class, TestStringRedisTemplate::new)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(RateLimitHandler.class)).isInstanceOf(RedisRateLimitHandler.class);
                });
    }

    @Test
    void missingExplicitRedisBeanShouldFailAtStartup() {
        contextRunner.withPropertyValues("velo.rate-limit.backend=redis")
                .run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(VeloProperties.class)
    static class PropertiesConfiguration {
    }
}
