package io.github.luminion.velo.cache;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RedisCacheTimeMapProviderTest {

    @Test
    void shouldOverrideTtlForNamedCaches() {
        RedisCacheTimeMapProvider provider = new RedisCacheTimeMapProvider(Map.of(
                "shortLived", Duration.ofSeconds(5),
                "longLived", Duration.ofMinutes(1)
        ));
        RedisCacheConfiguration baseConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(30));

        Map<String, RedisCacheConfiguration> configurations = provider.cacheConfigurationHashMap(baseConfiguration);

        assertThat(configurations.get("shortLived").getTtl()).isEqualTo(Duration.ofSeconds(5));
        assertThat(configurations.get("longLived").getTtl()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void shouldApplyJitterWithinExpectedRange() {
        Duration originalTtl = Duration.ofSeconds(100);
        int jitterPercentage = 20;

        for (int i = 0; i < 100; i++) {
            Duration jittered = RedisCacheTimeMapProvider.applyJitter(originalTtl, jitterPercentage);
            long millis = jittered.toMillis();
            assertThat(millis).isBetween(80_000L, 120_000L);
        }
    }

    @Test
    void shouldNotApplyJitterWhenPercentageIsZero() {
        Duration originalTtl = Duration.ofSeconds(60);

        Duration jittered = RedisCacheTimeMapProvider.applyJitter(originalTtl, 0);

        assertThat(jittered).isEqualTo(originalTtl);
    }

    @Test
    void shouldApplyJitterToPerCacheTtlWhenConfigured() {
        RedisCacheTimeMapProvider provider = new RedisCacheTimeMapProvider(
                Map.of("cache1", Duration.ofSeconds(100)),
                20
        );
        RedisCacheConfiguration baseConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(30));

        Map<String, RedisCacheConfiguration> configurations = provider.cacheConfigurationHashMap(baseConfiguration);

        Duration ttl = configurations.get("cache1").getTtl();
        assertThat(ttl.toMillis()).isBetween(80_000L, 120_000L);
    }
}
