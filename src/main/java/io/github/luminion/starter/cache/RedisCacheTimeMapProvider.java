package io.github.luminion.starter.cache;

import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

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
    private Map<String, Integer> cacheTimeMap;
    /**
     * redis缓存时间映射提供程序
     * 其中key为缓存名称，value为缓存时间(秒)
     *
     * @param cacheTimeMap 缓存时间映射
     */
    public RedisCacheTimeMapProvider(Map<String, Integer> cacheTimeMap) {
        this.cacheTimeMap = cacheTimeMap;
    }
    
    public Map<String, RedisCacheConfiguration> cacheConfigurationHashMap(RedisSerializer<Object> redisSerializer) {
        HashMap<String, RedisCacheConfiguration> cacheConfigurationHashMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : cacheTimeMap.entrySet()) {
            RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig();
            redisCacheConfiguration = redisCacheConfiguration
                    .computePrefixWith(cacheName -> cacheName)
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
                    .entryTtl(Duration.ofMinutes(5));
            cacheConfigurationHashMap.put(entry.getKey(), redisCacheConfiguration.entryTtl(Duration.ofSeconds(entry.getValue())));
        }

        return cacheConfigurationHashMap;
    }


}
