package io.github.luminion.velo.cache;

import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * redis缓存时间映射提供程序
 *
 * @author luminion
 * @since 1.0.0
 */
public class RedisCacheTimeMapProvider {
    private final Map<String, Duration> cacheTimeMap;
    private final int ttlJitterPercentage;

    /**
     * redis缓存时间映射提供程序
     * 其中key为缓存名称，value为缓存时间
     *
     * @param cacheTimeMap 缓存时间映射
     */
    public RedisCacheTimeMapProvider(Map<String, Duration> cacheTimeMap) {
        this(cacheTimeMap, 0);
    }

    /**
     * redis缓存时间映射提供程序
     * 其中key为缓存名称，value为缓存时间
     *
     * @param cacheTimeMap       缓存时间映射
     * @param ttlJitterPercentage TTL随机抖动百分比（0表示不抖动）
     */
    public RedisCacheTimeMapProvider(Map<String, Duration> cacheTimeMap, int ttlJitterPercentage) {
        this.cacheTimeMap = cacheTimeMap;
        this.ttlJitterPercentage = ttlJitterPercentage;
    }

    public Map<String, RedisCacheConfiguration> cacheConfigurationHashMap(RedisCacheConfiguration baseConfiguration) {
        HashMap<String, RedisCacheConfiguration> cacheConfigurationHashMap = new HashMap<>();
        for (Map.Entry<String, Duration> entry : cacheTimeMap.entrySet()) {
            Duration ttl = applyJitter(entry.getValue(), ttlJitterPercentage);
            cacheConfigurationHashMap.put(entry.getKey(), baseConfiguration.entryTtl(ttl));
        }

        return cacheConfigurationHashMap;
    }

    /**
     * 对TTL应用随机抖动，防止缓存雪崩。
     * <p>
     * 当jitterPercentage为10时，TTL将在原始值的±10%范围内随机变化。
     * 抖动在启动时计算一次，对同一缓存名称的所有条目生效。
     *
     * @param ttl              原始TTL
     * @param jitterPercentage 抖动百分比（0表示不抖动）
     * @return 抖动后的TTL
     */
    public static Duration applyJitter(Duration ttl, int jitterPercentage) {
        if (jitterPercentage <= 0 || ttl == null || ttl.isZero()) {
            return ttl;
        }
        double factor = 1.0 + (ThreadLocalRandom.current().nextDouble(-1.0, 1.0) * jitterPercentage / 100.0);
        long jitteredMillis = Math.max(1, (long) (ttl.toMillis() * factor));
        return Duration.ofMillis(jitteredMillis);
    }
}
