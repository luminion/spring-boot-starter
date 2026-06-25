package io.github.luminion.velo.cache;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VeloCacheAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    VeloCoreAutoConfiguration.class,
                    VeloCacheAutoConfiguration.class
            ))
            .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
            .withBean("redisSerializer", RedisSerializer.class, GenericJackson2JsonRedisSerializer::new);

    @Test
    void shouldCreateRedisCacheConfigurationUsingConfiguredPrefixAndTtl() {
        contextRunner
                .withPropertyValues(
                        "velo.cache.prefix=app",
                        "velo.cache.separator=:",
                        "velo.cache.default-ttl=42s"
                )
                .run(context -> {
                    assertThat(context.getBean(CacheManager.class)).isInstanceOf(RedisCacheManager.class);

                    RedisCacheConfiguration configuration = context.getBean(RedisCacheConfiguration.class);
                    assertThat(configuration.getKeyPrefixFor("users")).isEqualTo("app:users:");
                    assertThat(configuration.getTtl()).isEqualTo(Duration.ofSeconds(42));
                });
    }

    @Test
    void shouldDisableNullCachingWhenConfigured() {
        contextRunner
                .withPropertyValues(
                        "velo.cache.null-caching-enabled=false"
                )
                .run(context -> {
                    RedisCacheConfiguration configuration = context.getBean(RedisCacheConfiguration.class);
                    assertThat(configuration.getAllowCacheNullValues()).isFalse();
                });
    }

    @Test
    void shouldEnableNullCachingByDefault() {
        contextRunner
                .run(context -> {
                    RedisCacheConfiguration configuration = context.getBean(RedisCacheConfiguration.class);
                    assertThat(configuration.getAllowCacheNullValues()).isTrue();
                });
    }

    @Test
    void shouldKeepConfigurationTtlUnchangedRegardlessOfJitter() {
        // 抖动已移至 JitterRedisCacheWriter 在写入时按 key 应用，
        // RedisCacheConfiguration 上的 TTL 始终保持原始配置值。
        contextRunner
                .withPropertyValues(
                        "velo.cache.default-ttl=100s",
                        "velo.cache.ttl-jitter-percentage=20"
                )
                .run(context -> {
                    RedisCacheConfiguration configuration = context.getBean(RedisCacheConfiguration.class);
                    assertThat(configuration.getTtl()).isEqualTo(Duration.ofSeconds(100));
                });
    }

    @Test
    void shouldKeepDefaultTtlWhenNoJitterConfigured() {
        contextRunner
                .withPropertyValues(
                        "velo.cache.default-ttl=60s"
                )
                .run(context -> {
                    RedisCacheConfiguration configuration = context.getBean(RedisCacheConfiguration.class);
                    assertThat(configuration.getTtl()).isEqualTo(Duration.ofSeconds(60));
                });
    }
}
