package io.github.luminion.velo.jackson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.luminion.velo.redis.VeloRedisConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** 验证 Jackson 3 Redis JSON 序列化器的自动配置及用户 Bean 回退行为。 */
class VeloRedisJsonAutoConfigurationTests {

    /** 提供包含 Redis 配置的轻量应用上下文。 */
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    VeloRedisJsonAutoConfiguration.class, VeloRedisConfiguration.class))
            .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class));

    @Test
    void shouldSkipTheSerializerWhenTheOptionalRedisLibraryIsMissing() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("org.springframework.data.redis"))
                .withConfiguration(AutoConfigurations.of(VeloRedisJsonAutoConfiguration.class))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean("veloRedisSerializer"));
    }

    @Test
    void shouldRegisterOneDefaultSerializerWithoutMakingItPrimary() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(RedisSerializer.class);
            assertThat(context.getBeanFactory().getBeanDefinition("veloRedisSerializer").isPrimary()).isFalse();
            RedisSerializer<Object> serializer = context.getBean(RedisSerializer.class);
            Payload payload = new Payload();
            byte[] bytes = serializer.serialize(payload);
            String json = new String(bytes, StandardCharsets.UTF_8);
            assertThat(json).startsWith("{").contains("@class", "\"id\":9007199254740993", "\"title\":\"example\"")
                    .doesNotContain("ignored");
            Payload copy = (Payload) serializer.deserialize(bytes);
            assertThat(copy.id).isEqualTo(payload.id);
            assertThat(copy.amount).isEqualTo(payload.amount);
            assertThat(copy.createdAt).isEqualTo(payload.createdAt);
        });
    }

    @Test
    void shouldRestoreDtoCollectionsAndHashValuesWithTheSharedDefaultJsonSerializer() {
        runner.run(context -> {
            RedisSerializer<Object> defaults = context.getBean(RedisSerializer.class);
            for (String name : Arrays.asList("redisTemplate", "stringObjectRedisTemplate")) {
                RedisTemplate<?, ?> template = context.getBean(name, RedisTemplate.class);
                assertThat(template.getValueSerializer()).isSameAs(defaults);
                assertThat(template.getHashValueSerializer()).isSameAs(defaults);
                assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
                assertThat(template.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
                Payload source = new Payload();
                byte[] bytes = defaults.serialize(source);
                assertThat(new String(bytes, StandardCharsets.UTF_8)).startsWith("{")
                        .contains("@class", Payload.class.getName());
                Payload copy = (Payload) defaults.deserialize(bytes);
                assertThat(copy.id).isEqualTo(source.id);
                assertThat(copy.amount).isEqualTo(source.amount);
                assertThat(copy.createdAt).isEqualTo(source.createdAt);
                Object list = defaults.deserialize(defaults.serialize(new ArrayList<>(Arrays.asList(source))));
                assertThat((List<?>) list).hasSize(1).first().isInstanceOf(Payload.class);
                Map<String, Payload> map = new LinkedHashMap<>();
                map.put("one", source);
                Object restoredMap = defaults.deserialize(defaults.serialize(map));
                assertThat(restoredMap).isInstanceOf(Map.class);
                assertThat(((Map<?, ?>) restoredMap).get("one")).isInstanceOf(Payload.class);
                assertThat(defaults.deserialize(defaults.serialize("hello"))).isEqualTo("hello");
                assertThat(defaults.deserialize(defaults.serialize(null))).isNull();
            }
        });
    }

    @Test
    void shouldKeepTheDefaultWithDtoAndKeySerializersAndNotSelectThemForObjectValues() {
        runner.withUserConfiguration(LocalSerializers.class).run(context -> {
            assertThat(context).hasNotFailed().hasBean("veloRedisSerializer");
            RedisTemplate<?, ?> template = context.getBean("stringObjectRedisTemplate", RedisTemplate.class);
            assertThat(template.getValueSerializer()).isNotSameAs(context.getBean("dtoSerializer"))
                    .isNotSameAs(context.getBean("keySerializer"));
            RedisSerializer<Object> serializer = (RedisSerializer<Object>) template.getValueSerializer();
            assertThat(serializer.deserialize(serializer.serialize(new Payload()))).isInstanceOf(Payload.class);
        });
    }

    @Test
    void shouldUseTheUsersObjectSerializerRegardlessOfItsBeanName() {
        RedisSerializer<Object> user = RedisSerializer.json();
        runner.withBean("applicationValues", RedisSerializer.class, () -> user).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(RedisSerializer.class)
                    .doesNotHaveBean("veloRedisSerializer");
            assertThat(context.getBean("stringObjectRedisTemplate", RedisTemplate.class).getValueSerializer())
                    .isSameAs(user);
            assertThat(context.getBean("redisTemplate", RedisTemplate.class).getValueSerializer()).isSameAs(user);
        });
    }

    @Test
    void shouldBackOffForAnObjectSerializerDeclaredByAUserBeanMethod() {
        runner.withUserConfiguration(UserObjectSerializer.class).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(RedisSerializer.class)
                    .doesNotHaveBean("veloRedisSerializer");
            assertThat(context.getBean("redisTemplate", RedisTemplate.class).getValueSerializer())
                    .isSameAs(context.getBean("applicationValues"));
        });
    }

    @Test
    void shouldUsePrimaryAndFailForAmbiguousObjectSerializers() {
        RedisSerializer<Object> primary = RedisSerializer.json();
        runner.withBean("primary", RedisSerializer.class, () -> primary, definition -> definition.setPrimary(true))
                .withBean("other", RedisSerializer.class, RedisSerializer::json).run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean("redisTemplate", RedisTemplate.class).getValueSerializer())
                            .isSameAs(primary);
                });
        runner.withBean("redisSerializer", RedisSerializer.class, RedisSerializer::json)
                .withBean("second", RedisSerializer.class, RedisSerializer::json).run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(NoUniqueBeanDefinitionException.class);
                });
    }

    @Test
    void shouldFailWhenMoreThanOnePrimaryObjectSerializerExists() {
        runner.withBean("first", RedisSerializer.class, RedisSerializer::json, bean -> bean.setPrimary(true))
                .withBean("second", RedisSerializer.class, RedisSerializer::json, bean -> bean.setPrimary(true))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(NoUniqueBeanDefinitionException.class);
                });
    }

    @Test
    void shouldRetainRedisDateSupportWhenHttpEnhancementsAreDisabled() {
        runner.withPropertyValues("velo.jackson.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            RedisSerializer<Object> serializer = context.getBean(RedisSerializer.class);
            Payload copy = (Payload) serializer.deserialize(serializer.serialize(new Payload()));
            assertThat(copy.createdAt).isEqualTo(new Payload().createdAt);
        });
    }

    @Test
    void shouldRestorePolymorphicValuesAndSpringCacheNullMarker() {
        runner.run(context -> {
            RedisSerializer<Object> serializer = context.getBean(RedisSerializer.class);
            ObjectPayload copy = (ObjectPayload) serializer.deserialize(serializer.serialize(new ObjectPayload()));
            assertThat(copy.value).isInstanceOf(Payload.class);
            assertThat(((Payload) copy.value).createdAt).isEqualTo(new Payload().createdAt);
            Object restored = serializer.deserialize(serializer.serialize(
                    org.springframework.cache.support.NullValue.INSTANCE));
            assertThat(restored).isInstanceOf(org.springframework.cache.support.NullValue.class);
        });
    }

    /** 用于验证 Object 字段的多态类型信息能够恢复。 */
    public static final class ObjectPayload {

        /** 以运行时类型承载的嵌套负载。 */
        public Object value = new Payload();
    }

    @Configuration(proxyBeanMethods = false)
    static class UserObjectSerializer {
        @Bean
        RedisSerializer<Object> applicationValues() {
            return RedisSerializer.json();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class LocalSerializers {
        @Bean
        @Primary
        RedisSerializer<Payload> dtoSerializer() {
            return new JacksonJsonRedisSerializer<>(Payload.class);
        }
        @Bean
        RedisSerializer<String> keySerializer() {
            return StringRedisSerializer.UTF_8;
        }
    }

    /** Redis JSON 序列化行为测试使用的负载对象。 */
    public static final class Payload {

        /** 序列化时映射为 JSON 的 {@code title} 属性。 */
        @JsonProperty("title")
        public String name = "example";

        /** 标记为忽略、不会写入 JSON 的属性。 */
        @JsonIgnore
        public String ignored = "ignored";

        /** 超出 JavaScript 安全整数范围的标识值。 */
        public long id = 9007199254740993L;

        /** 用于验证精确小数值往返的金额。 */
        public BigDecimal amount = new BigDecimal("12.3400");

        /** 用于验证日期时间值往返的创建时间。 */
        public LocalDateTime createdAt = LocalDateTime.of(2026, 10, 7, 10, 20, 30, 123456789);
    }
}
