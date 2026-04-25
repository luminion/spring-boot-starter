package io.github.luminion.velo.lock.config;

import io.github.luminion.velo.core.VeloProperties;
import io.github.luminion.velo.core.spi.Fingerprinter;
import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.lock.VeloLockAutoConfiguration;
import io.github.luminion.velo.lock.aspect.LockAspect;
import io.github.luminion.velo.lock.support.CaffeineLockHandler;
import io.github.luminion.velo.lock.support.JdkLockHandler;
import io.github.luminion.velo.lock.support.RedisLockHandler;
import io.github.luminion.velo.lock.support.RedissonLockHandler;
import io.github.luminion.velo.test.TestStringRedisTemplate;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VeloLockAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    VeloLockRedissonAutoConfiguration.class,
                    VeloLockRedisAutoConfiguration.class,
                    VeloLockCaffeineAutoConfiguration.class,
                    VeloLockJdkAutoConfiguration.class
            ));

    @Test
    void shouldCreateDefaultLockHandler() {
        contextRunner
                .run(context -> assertThat(context.getBean(LockHandler.class))
                        .isInstanceOf(CaffeineLockHandler.class));
    }

    @Test
    void shouldPreferRedissonHandlerWhenRedissonAndRedisAreBothAvailable() {
        contextRunner
                .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withBean("stringRedisTemplate", StringRedisTemplate.class, TestStringRedisTemplate::new)
                .run(context -> assertThat(context.getBean(LockHandler.class))
                        .isInstanceOf(RedissonLockHandler.class));
    }

    @Test
    void shouldUseRedisHandlerWhenRedissonBackendIsDisabled() {
        contextRunner
                .withPropertyValues("velo.lock.backends.redisson-enabled=false")
                .withBean("stringRedisTemplate", StringRedisTemplate.class, TestStringRedisTemplate::new)
                .run(context -> assertThat(context.getBean(LockHandler.class))
                        .isInstanceOf(RedisLockHandler.class));
    }

    @Test
    void shouldUseJdkHandlerWhenCaffeineBackendIsDisabled() {
        contextRunner
                .withPropertyValues("velo.lock.backends.caffeine-enabled=false")
                .run(context -> assertThat(context.getBean(LockHandler.class))
                        .isInstanceOf(JdkLockHandler.class));
    }

    @Test
    void shouldUseExplicitBackendWhenConfigured() {
        contextRunner
                .withPropertyValues("velo.lock.backend=jdk")
                .run(context -> assertThat(context.getBean(LockHandler.class))
                        .isInstanceOf(JdkLockHandler.class));
    }

    @Test
    void shouldUseUserConfiguredHandlerWhenPresent() {
        contextRunner
                .withPropertyValues("velo.lock.backend=redisson")
                .withBean(LockHandler.class, CustomLockHandler::new)
                .run(context -> assertThat(context.getBean(LockHandler.class))
                        .isInstanceOf(CustomLockHandler.class));
    }

    @Test
    void shouldFailWhenExplicitBackendDependencyBeanIsMissing() {
        contextRunner
                .withPropertyValues("velo.lock.backend=redisson")
                .run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }

    @Test
    void shouldCreateAspectWhenCoreDependenciesExist() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        VeloLockRedissonAutoConfiguration.class,
                        VeloLockRedisAutoConfiguration.class,
                        VeloLockCaffeineAutoConfiguration.class,
                        VeloLockJdkAutoConfiguration.class,
                        VeloLockAutoConfiguration.class
                ))
                .withBean(VeloProperties.class, VeloProperties::new)
                .withBean(Fingerprinter.class, () -> (target, method, args, expression) -> "fingerprint")
                .run(context -> assertThat(context).hasSingleBean(LockAspect.class));
    }

    static class CustomLockHandler implements LockHandler {

        @Override
        public boolean lock(String key, long waitTime, long leaseTime, TimeUnit unit) {
            return true;
        }

        @Override
        public void unlock(String key) {
        }
    }
}
