package io.github.luminion.velo.cache;

import io.github.luminion.velo.VeloProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.couchbase.CouchbaseDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * spring缓存自动配置
 *
 * @author luminion
 * @see org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration
 * @since 1.0.0
 */
@AutoConfiguration(
        after = {
                CouchbaseDataAutoConfiguration.class,
                HazelcastAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                RedisAutoConfiguration.class
        },
        before = CacheAutoConfiguration.class
)
@ConditionalOnClass(CacheManager.class)
@ConditionalOnMissingBean(value = CacheManager.class, name = "cacheResolver")
@ConditionalOnProperty(prefix = "velo.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloCacheAutoConfiguration {

    private static String buildCacheKeyPrefix(VeloProperties.CacheProperties cacheProperties, String cacheName) {
        String separator = StringUtils.hasText(cacheProperties.getKeySeparator())
                ? cacheProperties.getKeySeparator()
                : ":";
        if (StringUtils.hasText(cacheProperties.getKeyPrefix())) {
            return cacheProperties.getKeyPrefix() + separator + cacheName + separator;
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
        @ConditionalOnProperty(prefix = "velo.cache", name = "redis-cache-time-map-provider-enabled", havingValue = "true", matchIfMissing = true)
        public RedisCacheTimeMapProvider redisCacheTimeMapProvider(VeloProperties properties) {
            return new RedisCacheTimeMapProvider(properties.getCache().getTtlMap());
        }

        @Bean
        @ConditionalOnMissingBean(RedisCacheConfiguration.class)
        @ConditionalOnProperty(prefix = "velo.cache", name = "redis-cache-configuration-enabled", havingValue = "true", matchIfMissing = true)
        public RedisCacheConfiguration redisCacheConfiguration(ObjectProvider<RedisSerializer<Object>> serializerProvider,
                VeloProperties properties) {
            RedisSerializer<Object> redisSerializer = serializerProvider.getIfAvailable(GenericJackson2JsonRedisSerializer::new);
            VeloProperties.CacheProperties cacheProperties = properties.getCache();

            RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig();
            RedisSerializationContext.SerializationPair<Object> objectSerializationPair = RedisSerializationContext
                    .SerializationPair
                    .fromSerializer(redisSerializer);

            return redisCacheConfiguration
                    .serializeValuesWith(objectSerializationPair)
                    .computePrefixWith(cacheName -> buildCacheKeyPrefix(cacheProperties, cacheName))
                    .entryTtl(Duration.ofSeconds(cacheProperties.getDefaultTtlSeconds()));
        }

        @Bean
        @ConditionalOnMissingBean(CacheManager.class)
        @ConditionalOnBean({RedisConnectionFactory.class, RedisCacheConfiguration.class, RedisCacheTimeMapProvider.class})
        @ConditionalOnProperty(prefix = "velo.cache", name = "cache-manager-enabled", havingValue = "true", matchIfMissing = true)
        public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory,
                                         RedisCacheConfiguration redisCacheConfiguration,
                                         RedisCacheTimeMapProvider redisCacheTimeMapProvider,
                                         ObjectProvider<RedisSerializer<Object>> serializerProvider) {
            RedisCacheManager redisCacheManager = new RedisCacheManager(
                    RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory),
                    redisCacheConfiguration,
                    redisCacheTimeMapProvider.cacheConfigurationHashMap(redisCacheConfiguration)
            );
            redisCacheManager.setTransactionAware(true);
            return redisCacheManager;
        }
    }
}
