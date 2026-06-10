package io.github.luminion.velo.redis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VeloRedisAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VeloRedisAutoConfiguration.class))
            .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class));

    @Test
    void shouldCreateRedisTemplatesWithJsonValueSerializer() {
        contextRunner
                .withBean("redisSerializer", RedisSerializer.class, GenericJackson2JsonRedisSerializer::new)
                .run(context -> {
                    assertThat(context).hasBean("redisTemplate");
                    assertThat(context).hasBean("stringObjectRedisTemplate");

                    RedisTemplate<Object, Object> redisTemplate = context.getBean("redisTemplate", RedisTemplate.class);
                    RedisTemplate<String, Object> stringObjectRedisTemplate = context.getBean("stringObjectRedisTemplate",
                            RedisTemplate.class);

                    assertThat(redisTemplate.getValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
                    assertThat(redisTemplate.getKeySerializer()).isEqualTo(StringRedisSerializer.UTF_8);
                    assertThat(stringObjectRedisTemplate.getValueSerializer())
                            .isInstanceOf(GenericJackson2JsonRedisSerializer.class);
                    assertThat(stringObjectRedisTemplate.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
                });
    }

    @Test
    void shouldCreateRedisTemplatesWithFallbackSerializerWhenRedisSerializerMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasBean("redisTemplate");
            assertThat(context).hasBean("stringObjectRedisTemplate");

            RedisTemplate<Object, Object> redisTemplate = context.getBean("redisTemplate", RedisTemplate.class);
            RedisTemplate<String, Object> stringObjectRedisTemplate = context.getBean("stringObjectRedisTemplate",
                    RedisTemplate.class);

            assertThat(redisTemplate.getValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
            assertThat(stringObjectRedisTemplate.getValueSerializer())
                    .isInstanceOf(GenericJackson2JsonRedisSerializer.class);
        });
    }

    @Test
    void shouldUseRedisSerializerRegisteredByLaterAutoConfiguration() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        VeloRedisAutoConfiguration.class,
                        LateRedisSerializerAutoConfiguration.class
                ))
                .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class));

        runner.run(context -> {
            RedisSerializer<?> redisSerializer = context.getBean("redisSerializer", RedisSerializer.class);
            RedisTemplate<Object, Object> redisTemplate = context.getBean("redisTemplate", RedisTemplate.class);
            RedisTemplate<String, Object> stringObjectRedisTemplate = context.getBean("stringObjectRedisTemplate",
                    RedisTemplate.class);

            assertThat(redisTemplate.getValueSerializer()).isSameAs(redisSerializer);
            assertThat(stringObjectRedisTemplate.getValueSerializer()).isSameAs(redisSerializer);
        });
    }

    @AutoConfiguration(after = VeloRedisAutoConfiguration.class)
    static class LateRedisSerializerAutoConfiguration {

        @Bean("redisSerializer")
        RedisSerializer<Object> redisSerializer() {
            return new MarkerRedisSerializer();
        }
    }

    static final class MarkerRedisSerializer implements RedisSerializer<Object> {

        @Override
        public byte[] serialize(Object value) throws SerializationException {
            return new byte[0];
        }

        @Override
        public Object deserialize(byte[] bytes) throws SerializationException {
            return null;
        }
    }
}
