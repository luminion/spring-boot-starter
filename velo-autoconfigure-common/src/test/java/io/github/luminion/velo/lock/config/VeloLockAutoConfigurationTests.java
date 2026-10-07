package io.github.luminion.velo.lock.config;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.lock.VeloLockAutoConfiguration;
import io.github.luminion.velo.lock.aspect.LockAspect;
import io.github.luminion.velo.lock.support.JdkLockHandler;
import io.github.luminion.velo.lock.support.RedisLockHandler;
import io.github.luminion.velo.lock.support.RedissonLockHandler;
import io.github.luminion.velo.test.TestStringRedisTemplate;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.concurrent.TimeUnit;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class VeloLockAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    VeloLockRedissonAutoConfiguration.class,
                    VeloLockRedisConfiguration.class,
                    VeloLockJdkAutoConfiguration.class,
                    VeloLockAutoConfiguration.class
            ))
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void shouldCreateDefaultLockHandler() {
        contextRunner
                .run(context -> assertThat(context.getBean(LockHandler.class))
                        .isInstanceOf(JdkLockHandler.class));
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
                .withPropertyValues("velo.lock.backend=redis")
                .withBean("stringRedisTemplate", StringRedisTemplate.class, TestStringRedisTemplate::new)
                .run(context -> assertThat(context.getBean(LockHandler.class))
                        .isInstanceOf(RedisLockHandler.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldPassConfiguredTtlSecondsToRedisBackend() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(eq("configured"), anyString(), eq(17L), eq(TimeUnit.SECONDS))).thenReturn(true);
        contextRunner.withPropertyValues("velo.lock.backend=redis", "velo.lock.redis-ttl-seconds=17")
                .withBean(StringRedisTemplate.class, () -> template)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    LockHandler handler = context.getBean(LockHandler.class);
                    assertThat(handler.tryLock("configured")).isTrue();
                    verify(values).setIfAbsent(eq("configured"), anyString(), eq(17L), eq(TimeUnit.SECONDS));
                    handler.unlock("configured");
                });
    }

    @Test
    void nonPositiveRedisTtlShouldFailAtStartup() {
        contextRunner.withPropertyValues("velo.lock.backend=redis", "velo.lock.redis-ttl-seconds=0")
                .withBean(StringRedisTemplate.class, TestStringRedisTemplate::new)
                .run(context -> assertThat(context.getStartupFailure())
                        .hasRootCauseInstanceOf(IllegalArgumentException.class)
                        .hasStackTraceContaining("TTL seconds"));
    }

    @Test
    void shouldUseCustomNamedStringRedisTemplateByType() {
        contextRunner
                .withPropertyValues("velo.lock.backend=redis")
                .withBean("businessRedisTemplate", StringRedisTemplate.class, TestStringRedisTemplate::new)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(LockHandler.class))
                            .isInstanceOf(RedisLockHandler.class);
                });
    }

    @Test
    void shouldUseCustomNamedStringRedisTemplateInAutoBackend() {
        contextRunner
                .withBean("businessRedisTemplate", StringRedisTemplate.class, TestStringRedisTemplate::new)
                .run(context -> assertThat(context.getBean(LockHandler.class))
                        .isInstanceOf(RedisLockHandler.class));
    }

    @Test
    void shouldPreferPrimaryStringRedisTemplateWhenMultipleCandidatesExist() {
        contextRunner
                .withPropertyValues("velo.lock.backend=redis")
                .withUserConfiguration(MultipleStringRedisTemplateConfiguration.class)
                .run(context -> {
                    RedisLockHandler handler = (RedisLockHandler) context.getBean(LockHandler.class);
                    assertThat(ReflectionTestUtils.getField(handler, "redisTemplate"))
                            .isSameAs(context.getBean("primaryStringRedisTemplate"));
                });
    }

    @Test
    void shouldUseDefaultStringRedisTemplateWhenMultipleCandidatesHaveNoPrimary() {
        contextRunner
                .withPropertyValues("velo.lock.backend=redis")
                .withBean("stringRedisTemplate", StringRedisTemplate.class, TestStringRedisTemplate::new)
                .withBean("secondaryStringRedisTemplate", StringRedisTemplate.class, TestStringRedisTemplate::new)
                .run(context -> {
                    RedisLockHandler handler = (RedisLockHandler) context.getBean(LockHandler.class);
                    assertThat(ReflectionTestUtils.getField(handler, "redisTemplate"))
                            .isSameAs(context.getBean("stringRedisTemplate"));
                });
    }

    @Test
    void shouldRejectRemovedCaffeineBackend() {
        contextRunner
                .withPropertyValues("velo.lock.backend=caffeine")
                .run(context -> assertThat(context.getStartupFailure())
                        .hasRootCauseInstanceOf(IllegalStateException.class)
                        .hasStackTraceContaining("velo.lock.backend=CAFFEINE"));
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
                        VeloLockRedisConfiguration.class,
                        VeloLockJdkAutoConfiguration.class,
                        VeloLockAutoConfiguration.class
                ))
                .withBean(VeloProperties.class, VeloProperties::new)
                .withBean(Fingerprinter.class, () -> (target, method, args, expression) -> "fingerprint")
                .run(context -> assertThat(context).hasSingleBean(LockAspect.class));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(VeloProperties.class)
    static class PropertiesConfiguration {
    }

    static class CustomLockHandler implements LockHandler {

        @Override
        public boolean tryLock(String key) {
            return true;
        }

        @Override
        public void unlock(String key) {
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class MultipleStringRedisTemplateConfiguration {

        @Bean
        @Primary
        StringRedisTemplate primaryStringRedisTemplate() {
            return new TestStringRedisTemplate();
        }

        @Bean
        StringRedisTemplate secondaryStringRedisTemplate() {
            return new TestStringRedisTemplate();
        }
    }
}
