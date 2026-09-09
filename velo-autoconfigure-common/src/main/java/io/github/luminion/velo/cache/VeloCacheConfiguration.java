package io.github.luminion.velo.cache;

import io.github.luminion.velo.VeloProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.StringUtils;

/**
 * 缓存配置实现，由各 Spring Boot 版本适配模块的自动配置入口导入。
 *
 * @author luminion
 * @since 1.3.1
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(CacheManager.class)
@ConditionalOnMissingBean(value = CacheManager.class, name = "cacheResolver")
@ConditionalOnProperty(prefix = "velo.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloCacheConfiguration {

    private static final Logger log = LoggerFactory.getLogger(VeloCacheConfiguration.class);

    private static String buildCacheKeyPrefix(VeloProperties.CacheProperties cacheProperties, String cacheName) {
        String separator = StringUtils.hasText(cacheProperties.getSeparator())
                ? cacheProperties.getSeparator()
                : ":";
        if (StringUtils.hasText(cacheProperties.getPrefix())) {
            return cacheProperties.getPrefix() + separator + cacheName + separator;
        }
        return cacheName + separator;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({RedisCacheManager.class, RedisConnectionFactory.class})
    // 兼容 spring.cache.type 配置
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    static class RedisCacheManagerConfiguration {

        @Bean
        @ConditionalOnMissingBean(RedisCacheTimeMapProvider.class)
        public RedisCacheTimeMapProvider redisCacheTimeMapProvider(VeloProperties properties) {
            // TTL 抖动统一由 JitterRedisCacheWriter 在写入时按 key 应用，
            // 这里不再预计算抖动，避免双重抖动。
            return new RedisCacheTimeMapProvider(properties.getCache().getTtl());
        }

        @Bean
        @ConditionalOnMissingBean(RedisCacheConfiguration.class)
        public RedisCacheConfiguration redisCacheConfiguration(ObjectProvider<RedisSerializer<Object>> serializerProvider,
                VeloProperties properties) {
            RedisSerializer<Object> redisSerializer = serializerProvider.getIfAvailable(() -> {
                log.debug("No RedisSerializer bean found, using RedisSerializer.json() for Redis cache values");
                return RedisSerializer.json();
            });
            VeloProperties.CacheProperties cacheProperties = properties.getCache();

            RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig();
            RedisSerializationContext.SerializationPair<Object> objectSerializationPair = RedisSerializationContext
                    .SerializationPair
                    .fromSerializer(redisSerializer);

            RedisCacheConfiguration config = redisCacheConfiguration
                    .serializeValuesWith(objectSerializationPair)
                    .computePrefixWith(cacheName -> buildCacheKeyPrefix(cacheProperties, cacheName))
                    .entryTtl(cacheProperties.getDefaultTtl());

            if (!cacheProperties.isNullCachingEnabled()) {
                config = config.disableCachingNullValues();
            }

            return config;
        }

        @Bean
        @ConditionalOnMissingBean(CacheManager.class)
        @ConditionalOnBean({RedisConnectionFactory.class, RedisCacheConfiguration.class, RedisCacheTimeMapProvider.class})
        public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory,
                RedisCacheConfiguration redisCacheConfiguration,
                RedisCacheTimeMapProvider redisCacheTimeMapProvider,
                VeloProperties properties) {
            // 每 key 独立抖动：包装 cache writer，在每次写入时对该条目的 TTL 叠加随机偏移，
            // 使同一缓存名称下不同 key 也获得不同过期时间，缓解同类型缓存批量同时过期。
            RedisCacheWriter cacheWriter = JitterRedisCacheWriter.wrap(
                    RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory),
                    properties.getCache().getTtlJitterPercentage());
            RedisCacheManager redisCacheManager = new RedisCacheManager(
                    cacheWriter,
                    redisCacheConfiguration,
                    redisCacheTimeMapProvider.cacheConfigurationHashMap(redisCacheConfiguration)
            );
            redisCacheManager.setTransactionAware(true);
            return redisCacheManager;
        }
    }
}
