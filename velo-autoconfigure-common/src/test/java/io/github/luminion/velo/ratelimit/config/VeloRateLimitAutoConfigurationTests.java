package io.github.luminion.velo.ratelimit.config;

import io.github.luminion.velo.core.VeloProperties;
import io.github.luminion.velo.core.spi.Fingerprinter;
import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.ratelimit.VeloRateLimitAutoConfiguration;
import io.github.luminion.velo.ratelimit.aspect.RateLimitAspect;
import io.github.luminion.velo.ratelimit.support.CaffeineRateLimitHandler;
import io.github.luminion.velo.ratelimit.support.RedisRateLimitHandler;
import io.github.luminion.velo.ratelimit.support.RedissonRateLimitHandler;
import io.github.luminion.velo.test.TestRedisTemplate;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VeloRateLimitAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    VeloRateLimitRedissonAutoConfiguration.class,
                    VeloRateLimitRedisAutoConfiguration.class,
                    VeloRateLimitCaffeineAutoConfiguration.class,
                    VeloRateLimitJdkAutoConfiguration.class
            ));

    @Test
    void shouldCreateDefaultRateLimitHandler() {
        contextRunner
                .run(context -> assertThat(context.getBean(RateLimitHandler.class))
                        .isInstanceOf(CaffeineRateLimitHandler.class));
    }

    @Test
    void shouldPreferRedissonHandlerWhenRedissonAndRedisAreBothAvailable() {
        contextRunner
                .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withBean("redisTemplate", RedisTemplate.class, TestRedisTemplate::new)
                .withBean("stringObjectRedisTemplate", RedisTemplate.class, TestRedisTemplate::new)
                .run(context -> assertThat(context.getBean(RateLimitHandler.class))
                        .isInstanceOf(RedissonRateLimitHandler.class));
    }

    @Test
    void shouldUseNamedRedisTemplateWhenRedissonBackendIsDisabled() {
        contextRunner
                .withPropertyValues("velo.rate-limit.backends.redisson-enabled=false")
                .withBean("redisTemplate", RedisTemplate.class, TestRedisTemplate::new)
                .withBean("stringObjectRedisTemplate", RedisTemplate.class, TestRedisTemplate::new)
                .run(context -> assertThat(context.getBean(RateLimitHandler.class))
                        .isInstanceOf(RedisRateLimitHandler.class));
    }

    @Test
    void shouldCreateAspectWhenCoreDependenciesExist() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        VeloRateLimitRedissonAutoConfiguration.class,
                        VeloRateLimitRedisAutoConfiguration.class,
                        VeloRateLimitCaffeineAutoConfiguration.class,
                        VeloRateLimitJdkAutoConfiguration.class,
                        VeloRateLimitAutoConfiguration.class
                ))
                .withBean(VeloProperties.class, VeloProperties::new)
                .withBean(Fingerprinter.class, () -> (target, method, args, expression) -> "fingerprint")
                .run(context -> assertThat(context).hasSingleBean(RateLimitAspect.class));
    }
}
