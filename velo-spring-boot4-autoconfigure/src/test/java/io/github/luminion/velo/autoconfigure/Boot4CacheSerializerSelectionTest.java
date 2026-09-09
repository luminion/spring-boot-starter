package io.github.luminion.velo.autoconfigure;

import io.github.luminion.velo.cache.VeloCacheAutoConfiguration;
import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.jackson.VeloJacksonAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证 Spring Boot 4 下缓存序列化器会尊重用户选择，并且无显式 Bean 时不强依赖 Jackson 2。
 *
 * @author luminion
 */
class Boot4CacheSerializerSelectionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    VeloCoreAutoConfiguration.class,
                    VeloCacheAutoConfiguration.class
            ))
            .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class));

    private final ApplicationContextRunner jacksonAwareContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    VeloCoreAutoConfiguration.class,
                    VeloCacheAutoConfiguration.class,
                    VeloJacksonAutoConfiguration.class
            ))
            .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class));

    @Test
    void shouldUseSpringDataJsonSerializerWhenNoSerializerBeanExists() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(CacheManager.class);

            Map<String, Object> payload = payload();
            RedisCacheConfiguration configuration = context.getBean(RedisCacheConfiguration.class);
            ByteBuffer serialized = configuration.getValueSerializationPair().write(payload);
            Object deserialized = configuration.getValueSerializationPair().read(serialized);

            assertThat(deserialized).isEqualTo(payload);
        });
    }

    @Test
    void shouldUseExplicitJackson2SerializerWhenUserProvidesIt() {
        RedisSerializer<Object> serializer = new GenericJackson2JsonRedisSerializer();

        jacksonAwareContextRunner
                .withBean("userJackson2Serializer", RedisSerializer.class, () -> serializer)
                .run(context -> {
                    assertThat(context).hasSingleBean(RedisSerializer.class);
                    RedisCacheConfiguration configuration = context.getBean(RedisCacheConfiguration.class);
                    byte[] actual = toByteArray(configuration.getValueSerializationPair().write(payload()));

                    assertThat(actual).isEqualTo(serializer.serialize(payload()));
                });
    }

    @Test
    void shouldUseExplicitJackson3SerializerWhenUserProvidesIt() {
        RedisSerializer<Object> serializer = RedisSerializer.json();

        jacksonAwareContextRunner
                .withBean("userJackson3Serializer", RedisSerializer.class, () -> serializer)
                .run(context -> {
                    assertThat(context).hasSingleBean(RedisSerializer.class);
                    RedisCacheConfiguration configuration = context.getBean(RedisCacheConfiguration.class);
                    byte[] actual = toByteArray(configuration.getValueSerializationPair().write(payload()));

                    assertThat(actual).isEqualTo(serializer.serialize(payload()));
                });
    }

    private static Map<String, Object> payload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", "ok");
        return payload;
    }

    private static byte[] toByteArray(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }
}
