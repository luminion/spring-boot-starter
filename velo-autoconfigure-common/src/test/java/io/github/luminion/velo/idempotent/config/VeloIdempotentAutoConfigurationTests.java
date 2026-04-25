package io.github.luminion.velo.idempotent.config;

import io.github.luminion.velo.idempotent.IdempotentHandler;
import io.github.luminion.velo.idempotent.VeloIdempotentAutoConfiguration;
import io.github.luminion.velo.idempotent.aspect.IdempotentAspect;
import io.github.luminion.velo.idempotent.support.CaffeineIdempotentHandler;
import io.github.luminion.velo.idempotent.support.JdkIdempotentHandler;
import io.github.luminion.velo.idempotent.support.RedisIdempotentHandler;
import io.github.luminion.velo.idempotent.support.RedissonIdempotentHandler;
import io.github.luminion.velo.test.TestRedisTemplate;
import io.github.luminion.velo.core.VeloProperties;
import io.github.luminion.velo.core.spi.Fingerprinter;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VeloIdempotentAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    VeloIdempotentRedissonAutoConfiguration.class,
                    VeloIdempotentRedisAutoConfiguration.class,
                    VeloIdempotentCaffeineAutoConfiguration.class,
                    VeloIdempotentJdkAutoConfiguration.class
            ));

    @Test
    void shouldCreateDefaultIdempotentHandler() {
        contextRunner
                .run(context -> assertThat(context.getBean(IdempotentHandler.class))
                        .isInstanceOf(CaffeineIdempotentHandler.class));
    }

    @Test
    void shouldPreferRedissonHandlerWhenRedissonAndRedisAreBothAvailable() {
        contextRunner
                .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withBean("redisTemplate", RedisTemplate.class, TestRedisTemplate::new)
                .withBean("stringObjectRedisTemplate", RedisTemplate.class, TestRedisTemplate::new)
                .run(context -> assertThat(context.getBean(IdempotentHandler.class))
                        .isInstanceOf(RedissonIdempotentHandler.class));
    }

    @Test
    void shouldUseNamedRedisTemplateWhenRedissonBackendIsDisabled() {
        contextRunner
                .withPropertyValues("velo.idempotent.backends.redisson-enabled=false")
                .withBean("redisTemplate", RedisTemplate.class, TestRedisTemplate::new)
                .withBean("stringObjectRedisTemplate", RedisTemplate.class, TestRedisTemplate::new)
                .run(context -> assertThat(context.getBean(IdempotentHandler.class))
                        .isInstanceOf(RedisIdempotentHandler.class));
    }

    @Test
    void shouldUseExplicitBackendWhenConfigured() {
        contextRunner
                .withPropertyValues("velo.idempotent.backend=jdk")
                .run(context -> assertThat(context.getBean(IdempotentHandler.class))
                        .isInstanceOf(JdkIdempotentHandler.class));
    }

    @Test
    void shouldUseUserConfiguredHandlerWhenPresent() {
        contextRunner
                .withPropertyValues("velo.idempotent.backend=redisson")
                .withBean(IdempotentHandler.class, CustomIdempotentHandler::new)
                .run(context -> assertThat(context.getBean(IdempotentHandler.class))
                        .isInstanceOf(CustomIdempotentHandler.class));
    }

    @Test
    void shouldFailWhenExplicitBackendDependencyBeanIsMissing() {
        contextRunner
                .withPropertyValues("velo.idempotent.backend=redisson")
                .run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }

    @Test
    void shouldCreateAspectWhenCoreDependenciesExist() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        VeloIdempotentRedissonAutoConfiguration.class,
                        VeloIdempotentRedisAutoConfiguration.class,
                        VeloIdempotentCaffeineAutoConfiguration.class,
                        VeloIdempotentJdkAutoConfiguration.class,
                        VeloIdempotentAutoConfiguration.class
                ))
                .withBean(VeloProperties.class, VeloProperties::new)
                .withBean(Fingerprinter.class, () -> (target, method, args, expression) -> "fingerprint")
                .run(context -> assertThat(context).hasSingleBean(IdempotentAspect.class));
    }

    static class CustomIdempotentHandler implements IdempotentHandler {

        @Override
        public boolean tryLock(String key, long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void unlock(String key) {
        }
    }
}
