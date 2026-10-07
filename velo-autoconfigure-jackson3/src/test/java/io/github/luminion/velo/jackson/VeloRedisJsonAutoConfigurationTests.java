package io.github.luminion.velo.jackson;

import io.github.luminion.velo.redis.RedisJsonSerializerFactory;
import io.github.luminion.velo.redis.VeloRedisConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VeloRedisJsonAutoConfigurationTests {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VeloRedisJsonAutoConfiguration.class, VeloRedisConfiguration.class))
            .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class));

    @Test
    void shouldSkipTheFactoryWhenTheOptionalRedisLibraryIsMissing() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("org.springframework.data.redis"))
                .withConfiguration(AutoConfigurations.of(VeloRedisJsonAutoConfiguration.class))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("redisJsonSerializerFactory"));
    }

    @Test
    void shouldUsePlainJsonAndRetainDatesNumbersAndGenericElementsForDeclaredTypes() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(RedisSerializer.class);
            RedisJsonSerializerFactory factory = context.getBean(RedisJsonSerializerFactory.class);
            Payload payload = new Payload();
            RedisSerializer<Object> typed = factory.create(Payload.class);
            byte[] bytes = typed.serialize(payload);
            String json = new String(bytes, StandardCharsets.UTF_8);
            assertThat(json).startsWith("{").contains("\"id\":9007199254740993").doesNotContain("@class", Payload.class.getName());
            Payload copy = (Payload) typed.deserialize(bytes);
            assertThat(copy.id).isEqualTo(payload.id);
            assertThat(copy.amount).isEqualTo(payload.amount);
            assertThat(copy.createdAt).isEqualTo(payload.createdAt);
            RedisSerializer<Object> list = factory.create(new ParameterizedTypeReference<List<Payload>>() { }.getType());
            assertThat((List<?>) list.deserialize(list.serialize(Arrays.asList(payload))))
                    .hasSize(1).first().isInstanceOf(Payload.class);
            RedisSerializer<Object> untyped = (RedisSerializer<Object>) context.getBean("stringObjectRedisTemplate", RedisTemplate.class).getValueSerializer();
            assertThat(untyped.deserialize(untyped.serialize(payload))).isInstanceOf(Map.class);
        });
    }

    @Test
    void shouldKeepTheFactoryWithDtoAndKeySerializersAndNotSelectThemForObjectValues() {
        runner.withUserConfiguration(LocalSerializers.class).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(RedisJsonSerializerFactory.class);
            RedisTemplate<?, ?> template = context.getBean("stringObjectRedisTemplate", RedisTemplate.class);
            assertThat(template.getValueSerializer()).isNotSameAs(context.getBean("dtoSerializer"))
                    .isNotSameAs(context.getBean("keySerializer"));
            RedisSerializer<Object> serializer = (RedisSerializer<Object>) template.getValueSerializer();
            assertThat(serializer.deserialize(serializer.serialize(new Payload()))).isInstanceOf(Map.class);
        });
    }

    @Test
    void shouldUseTheUsersObjectSerializerRegardlessOfItsBeanName() {
        RedisSerializer<Object> user = RedisSerializer.json();
        runner.withBean("applicationValues", RedisSerializer.class, () -> user).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(RedisSerializer.class);
            assertThat(context.getBean("stringObjectRedisTemplate", RedisTemplate.class).getValueSerializer()).isSameAs(user);
            assertThat(context.getBean("redisTemplate", RedisTemplate.class).getValueSerializer()).isSameAs(user);
        });
    }

    @Test
    void shouldUsePrimaryAndFailForAmbiguousObjectSerializers() {
        RedisSerializer<Object> primary = RedisSerializer.json();
        runner.withBean("primary", RedisSerializer.class, () -> primary, definition -> definition.setPrimary(true))
                .withBean("other", RedisSerializer.class, RedisSerializer::json).run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean("redisTemplate", RedisTemplate.class).getValueSerializer()).isSameAs(primary);
                });
        runner.withBean("first", RedisSerializer.class, RedisSerializer::json)
                .withBean("second", RedisSerializer.class, RedisSerializer::json).run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(NoUniqueBeanDefinitionException.class);
                });
    }

    @Test
    void shouldRetainRedisDateSupportWhenHttpEnhancementsAreDisabled() {
        runner.withPropertyValues("velo.jackson.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            RedisSerializer<Object> serializer = context.getBean(RedisJsonSerializerFactory.class).create(Payload.class);
            Payload copy = (Payload) serializer.deserialize(serializer.serialize(new Payload()));
            assertThat(copy.createdAt).isEqualTo(new Payload().createdAt);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class LocalSerializers {
        @Bean
        RedisSerializer<Payload> dtoSerializer(RedisJsonSerializerFactory factory) {
            return (RedisSerializer<Payload>) (RedisSerializer<?>) factory.create(Payload.class);
        }
        @Bean
        RedisSerializer<String> keySerializer() {
            return StringRedisSerializer.UTF_8;
        }
    }

    public static final class Payload {
        public long id = 9007199254740993L;
        public BigDecimal amount = new BigDecimal("12.3400");
        public LocalDateTime createdAt = LocalDateTime.of(2026, 10, 7, 10, 20, 30);
    }
}
