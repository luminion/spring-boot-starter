package io.github.luminion.velo.cache;

import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * redis缓存时间映射提供程序
 *
 * @author luminion
 * @since 1.0.0
 */
public class RedisCacheTimeMapProvider {
    private final Map<String, Integer> cacheTimeMap;
    /**
     * redis缓存时间映射提供程序
     * 其中key为缓存名称，value为缓存时间(秒)
     *
     * @param cacheTimeMap 缓存时间映射
     */
    public RedisCacheTimeMapProvider(Map<String, Integer> cacheTimeMap) {
        this.cacheTimeMap = cacheTimeMap;
    }

    public Map<String, RedisCacheConfiguration> cacheConfigurationHashMap(RedisCacheConfiguration baseConfiguration) {
        HashMap<String, RedisCacheConfiguration> cacheConfigurationHashMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : cacheTimeMap.entrySet()) {
            cacheConfigurationHashMap.put(entry.getKey(), baseConfiguration.entryTtl(Duration.ofSeconds(entry.getValue())));
        }

        return cacheConfigurationHashMap;
    }
}
