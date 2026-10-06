package io.github.luminion.velo.autoconfigure;

import io.github.luminion.velo.cache.JitterRedisCacheWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.cache.Cache;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 加载型 writer.get 的实际写入必须应用每个条目的 TTL 抖动。
 */
@EnabledIfEnvironmentVariable(named = "VELO_TEST_REDIS_URL", matches = "redis://.+")
class Boot3CacheJitterIntegrationTest {
    @Test
    void shouldJitterSynchronousCacheLoadsInRealRedis() {
        URI uri = URI.create(System.getenv("VELO_TEST_REDIS_URL"));
        int port = uri.getPort() < 0 ? 6379 : uri.getPort();
        LettuceConnectionFactory factory = new LettuceConnectionFactory(uri.getHost(), port);
        factory.afterPropertiesSet();
        factory.start();
        StringRedisTemplate template = new StringRedisTemplate(factory);
        String cacheName = "velo:integration:jitter:" + UUID.randomUUID();
        List<String> keys = new ArrayList<>();
        try {
            RedisCacheWriter writer = JitterRedisCacheWriter.wrap(RedisCacheWriter.nonLockingRedisCacheWriter(factory), 20);
            RedisCacheManager manager = RedisCacheManager.builder(writer)
                    .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(100)))
                    .build();
            manager.afterPropertiesSet();
            Cache cache = manager.getCache(cacheName);
            List<Long> ttls = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                String key = Integer.toString(i);
                String redisKey = cacheName + "::" + key;
                keys.add(redisKey);
                assertThat(cache.get(key, () -> "loaded")).isEqualTo("loaded");
                ttls.add(template.getExpire(redisKey, TimeUnit.MILLISECONDS));
            }
            assertThat(ttls).allSatisfy(ttl -> assertThat(ttl).isBetween(79000L, 120000L));
            assertThat(ttls.stream().distinct().count()).isGreaterThan(1);
            // 仅范围不足以发现固定 100 秒；还要确认实际偏离基准 TTL。
            assertThat(ttls.stream().anyMatch(ttl -> Math.abs(ttl - 100000L) > 2000L)).isTrue();
        } finally {
            template.delete(keys);
            factory.destroy();
        }
    }
}
