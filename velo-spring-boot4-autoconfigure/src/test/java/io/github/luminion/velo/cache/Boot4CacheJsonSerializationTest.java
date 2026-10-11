package io.github.luminion.velo.cache;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.jackson.VeloRedisJsonAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** 无需业务类型配置，验证默认 JSON 的类型恢复、动态缓存及原生缓存语义。 */
class Boot4CacheJsonSerializationTest {
    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                VeloCoreAutoConfiguration.class, VeloRedisJsonAutoConfiguration.class,
                VeloCacheAutoConfiguration.class, CacheAutoConfiguration.class))
                .withUserConfiguration(CachingConfiguration.class)
                .withPropertyValues("spring.cache.type=redis");
    }

    @Test
    void shouldRestoreFinalDtoAndJavaTimeWithoutTypeRegistration() {
        runner().withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
                .run(context -> {
                    RedisCacheConfiguration config = ((RedisCache) context.getBean(CacheManager.class)
                            .getCache("dto")).getCacheConfiguration();
                    Payload source = new Payload("one");
                    ByteBuffer bytes = config.getValueSerializationPair().write(source);
                    byte[] json = new byte[bytes.remaining()];
                    bytes.duplicate().get(json);
                    assertThat(new String(json, StandardCharsets.UTF_8))
                            .startsWith("{").contains("\"@class\"", Payload.class.getName());
                    Object restored = config.getValueSerializationPair().read(bytes);
                    assertThat(restored).isInstanceOf(Payload.class);
                    Payload copy = (Payload) restored;
                    assertThat(copy.name).isEqualTo(source.name);
                    assertThat(copy.id).isEqualTo(source.id);
                    assertThat(copy.amount).isEqualTo(source.amount);
                    assertThat(copy.createdAt).isEqualTo(source.createdAt);
                });
    }

    @Test
    void shouldRestoreDtoCollectionsAndDifferentValuesInTheSameCache() {
        runner().withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
                .run(context -> {
                    RedisCacheConfiguration config = ((RedisCache) context.getBean(CacheManager.class)
                            .getCache("mixed")).getCacheConfiguration();
                    List<Payload> values = new ArrayList<>(Arrays.asList(new Payload("list")));
                    Object list = config.getValueSerializationPair().read(config.getValueSerializationPair().write(values));
                    assertThat(list).isInstanceOf(List.class);
                    assertThat(((List<?>) list).get(0)).isInstanceOf(Payload.class);
                    assertThat(((Payload) ((List<?>) list).get(0)).name).isEqualTo("list");
                    Map<String, Payload> valuesById = new LinkedHashMap<>();
                    valuesById.put("one", new Payload("map"));
                    Object map = config.getValueSerializationPair().read(config.getValueSerializationPair().write(valuesById));
                    assertThat(map).isInstanceOf(Map.class);
                    assertThat(((Map<?, ?>) map).get("one")).isInstanceOf(Payload.class);
                    assertThat(((Payload) ((Map<?, ?>) map).get("one")).name).isEqualTo("map");
                    assertThat(config.getValueSerializationPair().read(config.getValueSerializationPair().write("text")))
                            .isEqualTo("text");
                });
    }

    @Test
    void shouldAllowPrecreatedNamedTtlAndDynamicCachesWithoutTypeRegistration() {
        runner().withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
                .withPropertyValues("spring.cache.cache-names=precreated", "spring.cache.redis.time-to-live=42s",
                        "spring.cache.redis.key-prefix=app:", "velo.cache.ttl.named=13s",
                        "spring.cache.redis.cache-null-values=false").run(context -> {
                    assertThat(context).hasNotFailed();
                    CacheManager manager = context.getBean(CacheManager.class);
                    assertThat(manager.getCacheNames()).contains("precreated", "named");
                    for (String name : Arrays.asList("precreated", "named", "dynamic")) {
                        Cache cache = manager.getCache(name);
                        assertThat(cache).isNotNull();
                        RedisCacheConfiguration config = ((RedisCache) cache).getCacheConfiguration();
                        Duration expectedTtl = Duration.ofSeconds("named".equals(name) ? 13 : 42);
                        assertThat(config.getTtlFunction().getTimeToLive("test", null)).isEqualTo(expectedTtl);
                        assertThat(config.getKeyPrefixFor(name)).isEqualTo("app:" + name + ":");
                        assertThat(config.getAllowCacheNullValues()).isFalse();
                    }
                });
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "VELO_TEST_REDIS_URL", matches = "redis://.+")
    void shouldHitDtoListNullAndDynamicCachesWithRealRedis() {
        URI uri = URI.create(System.getenv("VELO_TEST_REDIS_URL"));
        int port = uri.getPort() < 0 ? 6379 : uri.getPort();
        String prefix = "velo:json:boot4:" + UUID.randomUUID() + ":";
        runner().withBean(LettuceConnectionFactory.class, () -> new LettuceConnectionFactory(uri.getHost(), port))
                .withPropertyValues("spring.cache.redis.key-prefix=" + prefix, "velo.cache.ttl.dto=13s")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    StringRedisTemplate redis = new StringRedisTemplate(context.getBean(RedisConnectionFactory.class));
                    List<String> keys = Arrays.asList(prefix + "dto:first", prefix + "dto:empty",
                            prefix + "list:first", prefix + "dynamic:first");
                    try {
                        CachedService service = context.getBean(CachedService.class);
                        Payload first = service.dto("first");
                        Payload cached = service.dto("first");
                        assertThat(cached).isNotSameAs(first);
                        assertThat(cached.id).isEqualTo(first.id);
                        assertThat(cached.amount).isEqualTo(first.amount);
                        assertThat(cached.createdAt).isEqualTo(first.createdAt);
                        assertThat(service.dtoLoads.get()).isEqualTo(1);
                        assertThat(redis.opsForValue().get(keys.get(0))).startsWith("{")
                                .contains("\"@class\"", Payload.class.getName());
                        assertThat(redis.getExpire(keys.get(0), TimeUnit.MILLISECONDS)).isBetween(1000L, 13000L);
                        service.list("first");
                        assertThat(service.list("first")).hasSize(1).first().isInstanceOf(Payload.class);
                        assertThat(service.listLoads.get()).isEqualTo(1);
                        assertThat(service.dto("empty")).isNull();
                        assertThat(service.dto("empty")).isNull();
                        assertThat(service.dtoLoads.get()).isEqualTo(2);
                        Cache cache = context.getBean(CacheManager.class).getCache("dto");
                        assertThat(cache.get("empty")).isNotNull();
                        assertThat(cache.get("empty").get()).isNull();
                        service.dynamic("first");
                        assertThat(service.dynamic("first").name).isEqualTo("first");
                        assertThat(service.dynamicLoads.get()).isEqualTo(1);
                    } finally {
                        redis.delete(keys);
                    }
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class CachingConfiguration {
        @Bean
        CachedService cachedService() {
            return new CachedService();
        }
    }

    public static class CachedService {
        final AtomicInteger dtoLoads = new AtomicInteger();
        final AtomicInteger listLoads = new AtomicInteger();
        final AtomicInteger dynamicLoads = new AtomicInteger();

        @Cacheable(cacheNames = "dto", key = "#p0")
        public Payload dto(String key) {
            dtoLoads.incrementAndGet();
            return "empty".equals(key) ? null : new Payload(key);
        }

        @Cacheable(cacheNames = "list", key = "#p0")
        public List<Payload> list(String key) {
            listLoads.incrementAndGet();
            return new ArrayList<>(Arrays.asList(new Payload(key)));
        }

        @Cacheable(cacheNames = "dynamic", key = "#p0")
        public Payload dynamic(String key) {
            dynamicLoads.incrementAndGet();
            return new Payload(key);
        }
    }

    public static final class Payload {
        public String name;
        public long id = 9007199254740993L;
        public BigDecimal amount = new BigDecimal("12.3400");
        public LocalDateTime createdAt = LocalDateTime.of(2026, 10, 7, 10, 20, 30, 123456789);

        public Payload() {
        }

        public Payload(String name) {
            this.name = name;
        }
    }
}
