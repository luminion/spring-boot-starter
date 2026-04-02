package io.github.luminion.velo.redis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VeloRedisAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VeloRedisAutoConfiguration.class))
            .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
            .withBean("redisSerializer", RedisSerializer.class, GenericJackson2JsonRedisSerializer::new);

    @Test
    void shouldCreateRedisTemplatesWithJsonValueSerializer() {
        contextRunner.run(context -> {
            assertThat(context).hasBean("redisTemplate");
            assertThat(context).hasBean("stringObjectRedisTemplate");

            RedisTemplate<Object, Object> redisTemplate = context.getBean("redisTemplate", RedisTemplate.class);
            RedisTemplate<String, Object> stringObjectRedisTemplate = context.getBean("stringObjectRedisTemplate", RedisTemplate.class);

            assertThat(redisTemplate.getValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
            assertThat(redisTemplate.getKeySerializer()).isEqualTo(StringRedisSerializer.UTF_8);
            assertThat(stringObjectRedisTemplate.getValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
            assertThat(stringObjectRedisTemplate.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        });
    }

    @Test
    void shouldAllowDisablingSingleRedisTemplateBean() {
        contextRunner
                .withPropertyValues("velo.redis.redis-template-enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean("redisTemplate");
                    assertThat(context).hasBean("stringObjectRedisTemplate");
                });
    }
}
