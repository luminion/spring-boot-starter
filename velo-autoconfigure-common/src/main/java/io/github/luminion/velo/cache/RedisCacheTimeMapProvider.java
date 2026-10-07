package io.github.luminion.velo.cache;

import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.util.Assert;

/**
 * redis缓存时间映射提供程序
 * 其中key为缓存名称，value为缓存时间。
 * <p>
 * TTL 抖动不在此类处理，统一由 {@link JitterRedisCacheWriter} 在写入时按 key 应用。
 *
 * @author luminion
 * @since 1.0.0
 */
public class RedisCacheTimeMapProvider {
    private final Map<String, Duration> cacheTimeMap;

    /**
     * @param cacheTimeMap 缓存时间映射
     */
    public RedisCacheTimeMapProvider(Map<String, Duration> cacheTimeMap) {
        Assert.notNull(cacheTimeMap, "velo.cache.ttl 不能为 null");
        this.cacheTimeMap = cacheTimeMap;
    }

    public Map<String, RedisCacheConfiguration> cacheConfigurationHashMap(RedisCacheConfiguration baseConfiguration) {
        HashMap<String, RedisCacheConfiguration> cacheConfigurationHashMap = new HashMap<>();
        for (Map.Entry<String, Duration> entry : cacheTimeMap.entrySet()) {
            Assert.hasText(entry.getKey(), "velo.cache.ttl 的缓存名称不能为空");
            Duration ttl = VeloCacheConfiguration.validateTtl(entry.getValue(),
                    "velo.cache.ttl[" + entry.getKey() + "]");
            cacheConfigurationHashMap.put(entry.getKey(), baseConfiguration.entryTtl(ttl));
        }
        return cacheConfigurationHashMap;
    }
}
