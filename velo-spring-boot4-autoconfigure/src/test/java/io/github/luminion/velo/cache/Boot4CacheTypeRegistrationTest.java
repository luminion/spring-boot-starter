package io.github.luminion.velo.cache;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.jackson.VeloRedisJsonAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import io.github.luminion.velo.redis.RedisJsonSerializerFactory;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class Boot4CacheTypeRegistrationTest {
    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                VeloCoreAutoConfiguration.class, VeloRedisJsonAutoConfiguration.class,
                VeloCacheAutoConfiguration.class, CacheAutoConfiguration.class))
                .withUserConfiguration(TypesConfiguration.class)
                .withPropertyValues("spring.cache.type=redis");
    }

    @Test
    void shouldRegisterPlainJsonTypesAndPreserveTheSpringDefaultsAndNamedTtl() {
        runner().withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
                .withPropertyValues("spring.cache.cache-names=typed-list", "spring.cache.redis.time-to-live=42s",
                        "spring.cache.redis.key-prefix=app:", "velo.cache.ttl.typed=13s",
                        "spring.cache.redis.cache-null-values=false").run(context -> {
                    assertThat(context).hasNotFailed();
                    CacheManager manager = context.getBean(CacheManager.class);
                    RedisCacheConfiguration config = ((RedisCache) manager.getCache("typed")).getCacheConfiguration();
                    assertThat(config.getTtlFunction().getTimeToLive("test", null)).isEqualTo(Duration.ofSeconds(13));
                    assertThat(config.getKeyPrefixFor("typed")).isEqualTo("app:typed:");
                    assertThat(config.getAllowCacheNullValues()).isFalse();
                    ByteBuffer bytes = config.getValueSerializationPair().write(new Payload("one"));
                    assertThat(config.getValueSerializationPair().read(bytes)).isInstanceOf(Payload.class);
                    assertThat(((RedisCache) manager.getCache("typed-list")).getCacheConfiguration().getTtlFunction().getTimeToLive("test", null))
                            .isEqualTo(Duration.ofSeconds(42));
                    assertThat(manager.getCache("unknown")).isNull();
                });
    }

    @Test
    void shouldRejectConfiguredCacheNamesWithoutARegisteredType() {
        for (String property : new String[]{"spring.cache.cache-names=unknown", "velo.cache.ttl.unknown=13s"}) {
            runner().withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
                    .withPropertyValues(property).run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class)
                                .hasStackTraceContaining("unknown 未登记目标类型");
                    });
        }
    }

    @Test
    void shouldRunUserCustomizersAfterTheTypedDefaults() {
        runner().withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
                .withBean(RedisCacheManagerBuilderCustomizer.class, () -> builder -> {
                    RedisJsonSerializerFactory factory = new VeloRedisJsonAutoConfiguration().redisJsonSerializerFactory();
                    RedisCacheConfiguration custom = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(99))
                            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(factory.create(String.class)));
                    builder.withCacheConfiguration("typed", custom);
                }).run(context -> {
                    assertThat(context).hasNotFailed();
                    RedisCacheConfiguration config = ((RedisCache) context.getBean(CacheManager.class).getCache("typed"))
                            .getCacheConfiguration();
                    assertThat(config.getTtlFunction().getTimeToLive("test", null)).isEqualTo(Duration.ofSeconds(99));
                    assertThat(config.getValueSerializationPair().read(config.getValueSerializationPair().write("custom")))
                            .isEqualTo("custom");
                });
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "VELO_TEST_REDIS_URL", matches = "redis://.+")
    void shouldHitTypedDtoAndListCachesAndKeepNativeNullWithRealRedis() {
        URI uri = URI.create(System.getenv("VELO_TEST_REDIS_URL"));
        int port = uri.getPort() < 0 ? 6379 : uri.getPort();
        String prefix = "velo:typed:boot4:" + UUID.randomUUID() + ":";
        runner().withBean(LettuceConnectionFactory.class, () -> new LettuceConnectionFactory(uri.getHost(), port))
                .withPropertyValues("spring.cache.redis.key-prefix=" + prefix, "velo.cache.ttl.typed=13s")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    RedisConnectionFactory connectionFactory = context.getBean(RedisConnectionFactory.class);
                    StringRedisTemplate redis = new StringRedisTemplate(connectionFactory);
                    List<String> keys = Arrays.asList(prefix + "typed:first", prefix + "typed:empty", prefix + "typed-list:first");
                    try {
                        CachedService service = context.getBean(CachedService.class);
                        Payload first = service.dto("first");
                        Payload cached = service.dto("first");
                        assertThat(cached).isNotSameAs(first);
                        assertThat(cached.id).isEqualTo(9007199254740993L);
                        assertThat(cached.amount).isEqualTo(new BigDecimal("12.3400"));
                        assertThat(cached.createdAt).isEqualTo(first.createdAt);
                        assertThat(service.dtoLoadCount()).isEqualTo(1);
                        String json = redis.opsForValue().get(keys.get(0));
                        assertThat(json).startsWith("{").contains("\"id\":9007199254740993")
                                .doesNotContain("@class", Payload.class.getName());
                        assertThat(redis.getExpire(keys.get(0), TimeUnit.MILLISECONDS)).isBetween(1000L, 13000L);
                        service.list("first");
                        List<Payload> cachedList = service.list("first");
                        assertThat(cachedList).hasSize(1).first().isInstanceOf(Payload.class);
                        assertThat(service.listLoadCount()).isEqualTo(1);
                        assertThat(redis.opsForValue().get(keys.get(2))).startsWith("[{")
                                .doesNotContain("@class", Payload.class.getName());
                        assertThat(service.dto("empty")).isNull();
                        assertThat(service.dto("empty")).isNull();
                        assertThat(service.dtoLoadCount()).isEqualTo(2);
                        Cache cache = context.getBean(CacheManager.class).getCache("typed");
                        assertThat(cache.get("empty")).isNotNull();
                        assertThat(cache.get("empty").get()).isNull();
                        try (RedisConnection connection = connectionFactory.getConnection()) {
                            byte[] nullValue = connection.stringCommands().get(keys.get(1).getBytes(StandardCharsets.UTF_8));
                            assertThat(nullValue).startsWith((byte) 0xAC, (byte) 0xED);
                        }
                        assertThatThrownBy(() -> service.unknown("first")).isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("unknown");
                    } finally {
                        redis.delete(keys);
                    }
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class TypesConfiguration {
        @Bean
        RedisCacheTypeMapProvider cacheTypes() {
            Map<String, Type> types = new LinkedHashMap<>();
            types.put("typed", Payload.class);
            types.put("typed-list", new ParameterizedTypeReference<List<Payload>>() { }.getType());
            return new RedisCacheTypeMapProvider(types);
        }
        @Bean
        CachedService cachedService() {
            return new CachedService();
        }
    }

    public static class CachedService {
        final AtomicInteger dtoLoads = new AtomicInteger();
        final AtomicInteger listLoads = new AtomicInteger();
        public int dtoLoadCount() { return dtoLoads.get(); }
        public int listLoadCount() { return listLoads.get(); }
        @Cacheable(cacheNames = "typed", key = "#p0")
        public Payload dto(String key) {
            dtoLoads.incrementAndGet();
            return "empty".equals(key) ? null : new Payload(key);
        }
        @Cacheable(cacheNames = "typed-list", key = "#p0")
        public List<Payload> list(String key) {
            listLoads.incrementAndGet();
            return Arrays.asList(new Payload(key));
        }
        @Cacheable(cacheNames = "unknown", key = "#p0")
        public Payload unknown(String key) {
            return new Payload(key);
        }
    }

    public static final class Payload {
        public String name;
        public long id = 9007199254740993L;
        public BigDecimal amount = new BigDecimal("12.3400");
        public LocalDateTime createdAt = LocalDateTime.of(2026, 10, 7, 10, 20, 30);
        public Payload() { }
        public Payload(String name) { this.name = name; }
    }
}
