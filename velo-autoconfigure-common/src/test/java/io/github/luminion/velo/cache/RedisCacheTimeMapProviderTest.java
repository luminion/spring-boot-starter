package io.github.luminion.velo.cache;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RedisCacheTimeMapProviderTest {

    @Test
    void shouldOverrideTtlForNamedCaches() {
        Map<String, Duration> ttlMap = new LinkedHashMap<>();
        ttlMap.put("shortLived", Duration.ofSeconds(5));
        ttlMap.put("longLived", Duration.ofMinutes(1));
        RedisCacheTimeMapProvider provider = new RedisCacheTimeMapProvider(ttlMap);
        RedisCacheConfiguration baseConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(30));

        Map<String, RedisCacheConfiguration> configurations = provider.cacheConfigurationHashMap(baseConfiguration);

        assertThat(configurations.get("shortLived").getTtl()).isEqualTo(Duration.ofSeconds(5));
        assertThat(configurations.get("longLived").getTtl()).isEqualTo(Duration.ofSeconds(60));
    }
}
