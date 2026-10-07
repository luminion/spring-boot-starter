package io.github.luminion.velo.cache;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.jackson.VeloRedisJsonAutoConfiguration;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import static org.assertj.core.api.Assertions.assertThat;

/** 用真实 Redis 验证原生自动配置、统计、抖动、永久条目及批量清空的组合。 */
@EnabledIfEnvironmentVariable(named = "VELO_TEST_REDIS_URL", matches = "redis://.+")
class Boot3CacheRedisIntegrationTest {
    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class CachingEnabled {
    }

    @Test
    void shouldUseExplicitComponentsForSerializationTtlAndWrites() {
        URI uri = URI.create(System.getenv("VELO_TEST_REDIS_URL"));
        int port = uri.getPort() < 0 ? 6379 : uri.getPort();
        String prefix = "velo:audit:components:" + UUID.randomUUID() + ":";
        LettuceConnectionFactory factory = new LettuceConnectionFactory(uri.getHost(), port);
        RedisSerializer<Object> serializer = new JdkSerializationRedisSerializer();
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(name -> prefix + name + "/")
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .entryTtl((key, value) -> "short".equals(key) ? Duration.ofSeconds(5) : Duration.ofSeconds(30));
        RedisCacheWriter writer = RedisCacheWriter.nonLockingRedisCacheWriter(factory, BatchStrategies.keys());
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(VeloCoreAutoConfiguration.class,
                        VeloCacheAutoConfiguration.class, VeloRedisJsonAutoConfiguration.class, CacheAutoConfiguration.class))
                .withUserConfiguration(CachingEnabled.class)
                .withBean(LettuceConnectionFactory.class, () -> factory)
                .withBean(RedisCacheConfiguration.class, () -> defaults)
                .withBean(RedisCacheWriter.class, () -> writer)
                .withPropertyValues("spring.cache.type=redis", "spring.cache.redis.time-to-live=1s",
                        "spring.cache.redis.key-prefix=ignored:", "velo.cache.separator=ignored",
                        "velo.cache.ttl.mapped=8s").run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("veloRedisCacheWriter");
                    assertThat(context.getBean(RedisCacheWriter.class)).isSameAs(writer);
                    RedisCacheManager manager = context.getBean(RedisCacheManager.class);
                    assertThat(manager.isTransactionAware()).isFalse();
                    RedisTemplate<String, Object> reader = new RedisTemplate<>();
                    reader.setConnectionFactory(factory);
                    reader.setKeySerializer(RedisSerializer.string());
                    reader.setValueSerializer(serializer);
                    reader.afterPropertiesSet();
                    StringRedisTemplate redis = new StringRedisTemplate(factory);
                    Cache normal = manager.getCache("normal");
                    Cache mapped = manager.getCache("mapped");
                    List<String> keys = new ArrayList<>();
                    keys.add(prefix + "normal/short");
                    keys.add(prefix + "normal/long");
                    keys.add(prefix + "mapped/one");
                    try {
                        normal.put("short", "short-value");
                        normal.put("long", "long-value");
                        mapped.put("one", "mapped-value");
                        assertThat(normal.get("short", String.class)).isEqualTo("short-value");
                        assertThat(reader.opsForValue().get(prefix + "normal/long")).isEqualTo("long-value");
                        assertThat(reader.opsForValue().get(prefix + "mapped/one")).isEqualTo("mapped-value");
                        assertThat(redis.getExpire(prefix + "normal/short", TimeUnit.MILLISECONDS))
                                .isBetween(4000L, 5000L);
                        assertThat(redis.getExpire(prefix + "normal/long", TimeUnit.MILLISECONDS))
                                .isBetween(29000L, 30000L);
                        assertThat(redis.getExpire(prefix + "mapped/one", TimeUnit.MILLISECONDS))
                                .isBetween(7000L, 8000L);
                        normal.clear();
                        assertThat(redis.hasKey(prefix + "normal/short")).isFalse();
                        assertThat(redis.hasKey(prefix + "normal/long")).isFalse();
                        assertThat(mapped.get("one", String.class)).isEqualTo("mapped-value");
                    } finally {
                        redis.delete(keys);
                    }
                });
    }

    @Test
    void shouldKeepJitterWithStatisticsAndClearOnlyTheSelectedCache() {
        URI uri = URI.create(System.getenv("VELO_TEST_REDIS_URL"));
        int port = uri.getPort() < 0 ? 6379 : uri.getPort();
        String prefix = "velo:audit:fixed:" + UUID.randomUUID() + ":";
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(VeloCoreAutoConfiguration.class,
                        VeloCacheAutoConfiguration.class, VeloRedisJsonAutoConfiguration.class, CacheAutoConfiguration.class))
                .withUserConfiguration(CachingEnabled.class)
                .withBean(LettuceConnectionFactory.class, () -> new LettuceConnectionFactory(uri.getHost(), port))
                .withPropertyValues("spring.cache.type=redis", "spring.cache.cache-names=normal,persistent",
                        "spring.cache.redis.key-prefix=" + prefix, "spring.cache.redis.time-to-live=100s",
                        "spring.cache.redis.enable-statistics=true", "velo.cache.ttl-jitter-percentage=20",
                        "velo.cache.ttl.persistent=0").run(context -> {
                    assertThat(context).hasNotFailed();
                    RedisCacheManager manager = context.getBean(RedisCacheManager.class);
                    StringRedisTemplate redis = new StringRedisTemplate(context.getBean(LettuceConnectionFactory.class));
                    Cache normal = manager.getCache("normal");
                    Cache persistent = manager.getCache("persistent");
                    Cache separate = manager.getCache("separate");
                    List<String> keys = new ArrayList<>();
                    String outside = prefix + "outside:1";
                    keys.add(outside);
                    keys.add(prefix + "persistent:1");
                    keys.add(prefix + "separate:1");
                    keys.add(prefix + "normal:null");
                    try {
                        List<Long> ttls = new ArrayList<>();
                        for (int i = 0; i < 25; i++) {
                            String key = Integer.toString(i);
                            keys.add(prefix + "normal:" + key);
                            normal.put(key, "value-" + i);
                            assertThat(normal.get(key, String.class)).isEqualTo("value-" + i);
                            ttls.add(redis.getExpire(prefix + "normal:" + key, TimeUnit.MILLISECONDS));
                        }
                        assertThat(ttls).allSatisfy(ttl -> assertThat(ttl).isBetween(79000L, 120000L));
                        assertThat(ttls.stream().distinct().count()).isGreaterThan(1);
                        assertThat(ttls.stream().anyMatch(ttl -> Math.abs(ttl - 100000L) > 2000L)).isTrue();
                        Cache target = normal instanceof TransactionAwareCacheDecorator
                                ? ((TransactionAwareCacheDecorator) normal).getTargetCache() : normal;
                        assertThat(((RedisCache) target).getStatistics().getHits()).isGreaterThanOrEqualTo(25L);
                        normal.put("null", null);
                        assertThat(normal.get("null")).isNotNull();
                        assertThat(normal.get("null").get()).isNull();
                        persistent.put("1", "permanent");
                        assertThat(redis.getExpire(prefix + "persistent:1", TimeUnit.MILLISECONDS)).isEqualTo(-1L);
                        separate.put("1", "other-cache");
                        redis.opsForValue().set(outside, "outside");
                        normal.clear();
                        for (int i = 0; i < 25; i++) {
                            assertThat(redis.hasKey(prefix + "normal:" + i)).isFalse();
                        }
                        assertThat(redis.hasKey(prefix + "normal:null")).isFalse();
                        assertThat(persistent.get("1", String.class)).isEqualTo("permanent");
                        assertThat(separate.get("1", String.class)).isEqualTo("other-cache");
                        assertThat(redis.opsForValue().get(outside)).isEqualTo("outside");
                    } finally {
                        // 只删除本用例拥有的键；清理在上下文关闭连接工厂之前完成。
                        redis.delete(keys);
                    }
                });
    }
}
