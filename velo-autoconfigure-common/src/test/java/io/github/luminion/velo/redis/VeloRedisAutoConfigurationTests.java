package io.github.luminion.velo.redis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
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
            .withConfiguration(AutoConfigurations.of(VeloRedisConfiguration.class))
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
    void shouldCreateRedisTemplatesWithJsonFallbackWhenRedisSerializerMissing() {
        // 无 redisSerializer bean 时，fallback 使用带类型信息的 JSON 序列化，保证普通 JavaBean 可反序列化回原类型。
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
    void shouldRoundTripJsonFallbackSerializerForPojo() {
        contextRunner.run(context -> {
            RedisSerializer<Object> serializer = context.getBean("redisTemplate", RedisTemplate.class)
                    .getValueSerializer();
            Object value = serializer.deserialize(serializer.serialize(new JsonPayload("ok")));

            assertThat(value).isInstanceOf(JsonPayload.class);
            assertThat(((JsonPayload) value).getName()).isEqualTo("ok");
        });
    }

    @Test
    void shouldSkipRedisTemplatesWhenRedisConnectionFactoryMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(VeloRedisConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("redisTemplate");
                    assertThat(context).doesNotHaveBean("stringObjectRedisTemplate");
                    assertThat(context).doesNotHaveBean("stringRedisTemplate");
                });
    }

    @Test
    void shouldSkipRedisTemplatesWhenRedisConnectionFactoriesAreAmbiguous() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        VeloRedisConfiguration.class,
                        RedisAutoConfiguration.class
                ))
                .withBean("primaryRedisConnectionFactory", RedisConnectionFactory.class,
                        () -> mock(RedisConnectionFactory.class))
                .withBean("secondaryRedisConnectionFactory", RedisConnectionFactory.class,
                        () -> mock(RedisConnectionFactory.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("redisTemplate");
                    assertThat(context).doesNotHaveBean("stringObjectRedisTemplate");
                    assertThat(context).doesNotHaveBean("stringRedisTemplate");
                });
    }

    @Test
    void shouldUseRedisSerializerRegisteredByLaterAutoConfiguration() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        VeloRedisConfiguration.class,
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

    @Test
    void shouldOverrideBootRedisTemplateWithJsonSerializerInsteadOfJdk() {
        // 回归：velo 必须排在官方 RedisAutoConfiguration 之前，否则官方 JDK 序列化的 redisTemplate 先占名，
        // velo 版被同名条件跳过，应用注入到 JDK 模板，写非 Serializable 对象即抛 SerializationException。
        // 提供 redisSerializer bean 模拟 velo.jackson 生效的真实场景，验证 velo 用它而非退回 JDK。
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        VeloRedisConfiguration.class,
                        RedisAutoConfiguration.class
                ))
                .withBean("redisSerializer", RedisSerializer.class, GenericJackson2JsonRedisSerializer::new)
                .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
                .run(context -> {
                    RedisTemplate<Object, Object> redisTemplate = context.getBean("redisTemplate", RedisTemplate.class);
                    assertThat(redisTemplate.getValueSerializer())
                            .isInstanceOf(GenericJackson2JsonRedisSerializer.class);
                    assertThat(redisTemplate.getHashValueSerializer())
                            .isInstanceOf(GenericJackson2JsonRedisSerializer.class);

                    RedisTemplate<String, Object> stringObjectRedisTemplate = context.getBean("stringObjectRedisTemplate",
                            RedisTemplate.class);
                    assertThat(stringObjectRedisTemplate.getValueSerializer())
                            .isInstanceOf(GenericJackson2JsonRedisSerializer.class);
                });
    }

    @AutoConfiguration(after = VeloRedisConfiguration.class)
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

    static final class JsonPayload {

        private String name;

        JsonPayload() {
        }

        JsonPayload(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
