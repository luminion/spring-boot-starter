package io.github.luminion.velo.cache;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** 验证真实 Boot 原生创建流程与 Velo 默认增强之间的配置优先级。 */
class Boot2CacheAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VeloCoreAutoConfiguration.class,
                    VeloCacheAutoConfiguration.class, CacheAutoConfiguration.class))
            .withUserConfiguration(CachingEnabled.class)
            .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
            .withPropertyValues("spring.cache.type=redis");

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class CachingEnabled {
    }

    @Test
    void shouldUseJsonFiveMinuteTtlAndSingleColonByDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(CacheManager.class);
            assertThat(context.getBean(RedisCacheManager.class).isTransactionAware()).isFalse();
            RedisCacheConfiguration config = context.getBean(RedisCacheConfiguration.class);
            assertThat(config.getTtl()).isEqualTo(Duration.ofMinutes(5));
            assertThat(config.getAllowCacheNullValues()).isTrue();
            assertThat(config.getKeyPrefixFor("users")).isEqualTo("users:");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("name", "ok");
            ByteBuffer serialized = config.getValueSerializationPair().write(payload);
            assertThat(config.getValueSerializationPair().read(serialized)).isEqualTo(payload);
        });
    }

    @Test
    void shouldApplySpringPropertiesToPrecreatedAndDynamicCaches() {
        runner.withPropertyValues("spring.cache.redis.time-to-live=42s",
                "spring.cache.redis.key-prefix=app:", "spring.cache.redis.cache-null-values=false",
                "spring.cache.cache-names=users", "velo.cache.ttl.orders=9s").run(context -> {
            assertThat(context).hasNotFailed();
            RedisCacheManager manager = context.getBean(RedisCacheManager.class);
            assertThat(manager.getCacheNames()).contains("users", "orders");
            for (String name : new String[]{"users", "dynamic"}) {
                RedisCacheConfiguration config = redisCache(manager.getCache(name)).getCacheConfiguration();
                assertThat(config.getTtl()).isEqualTo(Duration.ofSeconds(42));
                assertThat(config.getAllowCacheNullValues()).isFalse();
                assertThat(config.getKeyPrefixFor(name)).isEqualTo("app:" + name + ":");
            }
            assertThat(redisCache(manager.getCache("orders")).getCacheConfiguration().getTtl())
                    .isEqualTo(Duration.ofSeconds(9));
        });
    }

    @Test
    void shouldHonorDisabledKeyPrefix() {
        runner.withPropertyValues("spring.cache.redis.use-key-prefix=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(RedisCacheConfiguration.class).usePrefix()).isFalse();
        });
    }

    @Test
    void shouldRespectCustomSeparator() {
        runner.withPropertyValues("spring.cache.redis.key-prefix=app:", "velo.cache.separator=::").run(context ->
                assertThat(context.getBean(RedisCacheConfiguration.class).getKeyPrefixFor("users"))
                        .isEqualTo("app:users::"));
    }

    @Test
    void shouldCallUserCustomizersAfterVeloDefaults() {
        AtomicBoolean builderCalled = new AtomicBoolean();
        AtomicBoolean managerCalled = new AtomicBoolean();
        runner.withPropertyValues("velo.cache.ttl.orders=9s", "velo.cache.transaction-aware=true")
                .withBean(RedisCacheManagerBuilderCustomizer.class, () -> builder -> {
                    builderCalled.set(true);
                    builder.withCacheConfiguration("orders",
                            RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(17)));
                })
                .withBean(CacheManagerCustomizer.class, () -> (CacheManagerCustomizer<RedisCacheManager>) manager -> {
                    managerCalled.set(true);
                    manager.setTransactionAware(false);
                }).run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(builderCalled).isTrue();
                    assertThat(managerCalled).isTrue();
                    RedisCacheManager manager = context.getBean(RedisCacheManager.class);
                    assertThat(manager.isTransactionAware()).isFalse();
                    assertThat(redisCache(manager.getCache("orders")).getCacheConfiguration().getTtl())
                            .isEqualTo(Duration.ofSeconds(17));
                });
    }

    @Test
    void shouldRespectUserConfigurationAndSerializerBeans() {
        RedisSerializer<Object> serializer = RedisSerializer.json();
        RedisCacheConfiguration userDefaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(37)).computePrefixWith(name -> "custom/" + name + "/")
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new JdkSerializationRedisSerializer()));
        runner.withPropertyValues("spring.cache.redis.time-to-live=1s", "spring.cache.redis.key-prefix=ignored:",
                        "spring.cache.redis.use-key-prefix=false", "spring.cache.redis.cache-null-values=false")
                .withBean(RedisCacheConfiguration.class, () -> userDefaults)
                .withBean("firstSerializer", RedisSerializer.class, RedisSerializer::json)
                .withBean("secondSerializer", RedisSerializer.class, RedisSerializer::json)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(RedisCacheConfiguration.class)).isSameAs(userDefaults);
                    RedisCacheConfiguration config = redisCache(context.getBean(CacheManager.class).getCache("user"))
                            .getCacheConfiguration();
                    assertThat(config.getTtl()).isEqualTo(Duration.ofSeconds(37));
                    assertThat(config.getKeyPrefixFor("user")).isEqualTo("custom/user/");
                    assertThat(config.getAllowCacheNullValues()).isTrue();
                    assertThat(config.getValueSerializationPair().write("value"))
                            .isEqualTo(userDefaults.getValueSerializationPair().write("value"));
                });
        runner.withBean("userSerializer", RedisSerializer.class, () -> serializer).run(context -> {
            RedisCacheConfiguration config = context.getBean(RedisCacheConfiguration.class);
            assertThat(config.getValueSerializationPair().write("value")).isEqualTo(ByteBuffer.wrap(serializer.serialize("value")));
        });
    }

    @Test
    void shouldUseUserWriterForCacheWrites() {
        RedisCacheWriter writer = mock(RedisCacheWriter.class);
        runner.withBean("userWriter", RedisCacheWriter.class, () -> writer).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(RedisCacheWriter.class);
            assertThat(context).doesNotHaveBean("veloRedisCacheWriter");
            context.getBean(CacheManager.class).getCache("users").put("one", "value");
            verify(writer).put(eq("users"), eq("users:one".getBytes(StandardCharsets.UTF_8)),
                    any(byte[].class), eq(Duration.ofMinutes(5)));
        });
    }

    @Test
    void shouldSelectPrimaryWriter() {
        RedisCacheWriter primary = mock(RedisCacheWriter.class);
        RedisCacheWriter other = mock(RedisCacheWriter.class);
        runner.withBean("primaryWriter", RedisCacheWriter.class, () -> primary, definition -> definition.setPrimary(true))
                .withBean("otherWriter", RedisCacheWriter.class, () -> other).run(context -> {
                    assertThat(context).hasNotFailed();
                    context.getBean(CacheManager.class).getCache("users").put("one", "value");
                    verify(primary).put(eq("users"), any(byte[].class), any(byte[].class), any(Duration.class));
                    verifyNoInteractions(other);
                });
    }

    @Test
    void shouldRejectAmbiguousWriters() {
        runner.withBean("firstWriter", RedisCacheWriter.class, () -> mock(RedisCacheWriter.class))
                .withBean("secondWriter", RedisCacheWriter.class, () -> mock(RedisCacheWriter.class)).run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(NoUniqueBeanDefinitionException.class);
                });
    }

    @Test
    void shouldSelectPrimaryValueSerializer() {
        RedisSerializer<Object> primary = new JdkSerializationRedisSerializer();
        runner.withBean("primarySerializer", RedisSerializer.class, () -> primary,
                        definition -> definition.setPrimary(true))
                .withBean("otherSerializer", RedisSerializer.class, RedisSerializer::json).run(context -> {
                    assertThat(context).hasNotFailed();
                    RedisCacheConfiguration config = context.getBean(RedisCacheConfiguration.class);
                    assertThat(config.getValueSerializationPair().write("value"))
                            .isEqualTo(ByteBuffer.wrap(primary.serialize("value")));
                });
    }

    @Test
    void shouldRejectAmbiguousValueSerializers() {
        runner.withBean("firstSerializer", RedisSerializer.class, RedisSerializer::json)
                .withBean("secondSerializer", RedisSerializer.class, RedisSerializer::json).run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(NoUniqueBeanDefinitionException.class);
                });
    }

    @Test
    void shouldEnableTransactionAwarenessWhenRequested() {
        runner.withPropertyValues("velo.cache.transaction-aware=true").run(context -> {
            assertThat(context).hasNotFailed();
            RedisCacheManager manager = context.getBean(RedisCacheManager.class);
            assertThat(manager.isTransactionAware()).isTrue();
            assertThat(manager.getCache("users")).isInstanceOf(TransactionAwareCacheDecorator.class);
        });
    }

    @Test
    void shouldLetUserBuilderReplaceTheWriter() {
        RedisCacheWriter writer = mock(RedisCacheWriter.class);
        runner.withBean(RedisCacheManagerBuilderCustomizer.class, () -> builder -> builder.cacheWriter(writer))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    context.getBean(CacheManager.class).getCache("users").put("one", "value");
                    verify(writer).put(eq("users"), any(byte[].class), any(byte[].class), any(Duration.class));
                });
    }

    @Test
    void shouldUseUserTtlMapProvider() {
        runner.withPropertyValues("velo.cache.ttl.orders=99s")
                .withBean(RedisCacheTimeMapProvider.class,
                        () -> new RedisCacheTimeMapProvider(Collections.singletonMap("orders", Duration.ofSeconds(11))))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    RedisCacheConfiguration config = redisCache(context.getBean(CacheManager.class)
                            .getCache("orders")).getCacheConfiguration();
                    assertThat(config.getTtl()).isEqualTo(Duration.ofSeconds(11));
                });
    }

    @Test
    void shouldAllowZeroAndMillisecondTtl() {
        for (String ttl : new String[]{"0", "0s", "1ms"}) {
            runner.withPropertyValues("spring.cache.redis.time-to-live=" + ttl, "velo.cache.ttl.orders=" + ttl)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        Duration expected = ttl.equals("1ms") ? Duration.ofMillis(1) : Duration.ZERO;
                        assertThat(context.getBean(RedisCacheConfiguration.class).getTtl()).isEqualTo(expected);
                        assertThat(redisCache(context.getBean(CacheManager.class).getCache("orders"))
                                .getCacheConfiguration().getTtl()).isEqualTo(expected);
                    });
        }
    }

    @Test
    void shouldFailStartupForInvalidDefaultOrNamedTtl() {
        for (String property : new String[]{"spring.cache.redis.time-to-live", "velo.cache.ttl.orders"}) {
            for (String ttl : new String[]{"-1", "-1s", "PT0.000001S", "PT9223372036854775807S"}) {
                runner.withPropertyValues(property + "=" + ttl).run(context -> {
                    assertThat(context).hasFailed();
                    String expectedProperty = property.startsWith("velo") ? "velo.cache.ttl[orders]" : property;
                    assertThat(context.getStartupFailure()).hasStackTraceContaining(expectedProperty);
                });
            }
        }
    }

    @Test
    void shouldBackOffForUserManagerAndOtherCacheTypes() {
        runner.withBean(CacheManager.class, SimpleCacheManager::new).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(CacheManager.class);
            assertThat(context).doesNotHaveBean(RedisCacheConfiguration.class);
        });
        runner.withPropertyValues("spring.cache.type=none").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(RedisCacheConfiguration.class);
        });
    }

    @Test
    void shouldRestoreBootDefaultsWhenEnhancementDisabled() {
        runner.withPropertyValues("velo.cache.enabled=false", "spring.cache.redis.time-to-live=2s")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(RedisCacheTimeMapProvider.class);
                    RedisCacheConfiguration config = redisCache(context.getBean(CacheManager.class).getCache("users"))
                            .getCacheConfiguration();
                    assertThat(config.getTtl()).isEqualTo(Duration.ofSeconds(2));
                    assertThat(config.getKeyPrefixFor("users")).isEqualTo("users::");
                });
    }

    @Test
    void shouldSkipEnhancementWhenRedisAbsent() {
        new ApplicationContextRunner().withClassLoader(new FilteredClassLoader("org.springframework.data.redis"))
                .withConfiguration(AutoConfigurations.of(VeloCoreAutoConfiguration.class, VeloCacheAutoConfiguration.class))
                .run(context -> assertThat(context).hasNotFailed());
    }
    private RedisCache redisCache(Cache cache) {
        if (cache instanceof TransactionAwareCacheDecorator) {
            cache = ((TransactionAwareCacheDecorator) cache).getTargetCache();
        }
        return (RedisCache) cache;
    }
}
