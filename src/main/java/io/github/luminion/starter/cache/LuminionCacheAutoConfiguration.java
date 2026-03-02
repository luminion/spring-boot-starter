package io.github.luminion.starter.cache;

import org.redisson.spring.starter.RedissonAutoConfiguration;
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
import org.springframework.cache.interceptor.CacheAspectSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.HashMap;

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
@ConditionalOnBean(CacheAspectSupport.class)
@ConditionalOnMissingBean(value = CacheManager.class, name = "cacheResolver")
public class LuminionCacheAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({RedisCacheManager.class, RedisConnectionFactory.class})
    // 兼容 spring.cache.type 配置
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    static class RedisCacheManagerConfiguration {

        @Bean
        @ConditionalOnMissingBean(RedisCacheTimeMapProvider.class)
        public RedisCacheTimeMapProvider redisCacheTimeMapProvider() {
            return new RedisCacheTimeMapProvider(new HashMap<>());
        }

        @Bean
        @ConditionalOnMissingBean(RedisCacheConfiguration.class)
        // 【关键修复2】：去掉 @ConditionalOnBean(RedisSerializer.class)，改用 ObjectProvider
        public RedisCacheConfiguration redisCacheConfiguration(ObjectProvider<RedisSerializer<Object>> serializerProvider) {

            // 如果用户容器里有 RedisSerializer 就用用户的，没有就提供一个默认的（比如 GenericJackson2JsonRedisSerializer）
            RedisSerializer<Object> redisSerializer = serializerProvider.getIfAvailable(GenericJackson2JsonRedisSerializer::new);

            RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig();
            RedisSerializationContext.SerializationPair<Object> objectSerializationPair = RedisSerializationContext
                    .SerializationPair
                    .fromSerializer(redisSerializer);

            return redisCacheConfiguration
                    .serializeValuesWith(objectSerializationPair)
                    .computePrefixWith(cacheName -> cacheName)
                    .entryTtl(Duration.ofSeconds(3000));
        }

        @Bean
        @ConditionalOnMissingBean(CacheManager.class)
        @ConditionalOnBean({RedisConnectionFactory.class, RedisCacheConfiguration.class, RedisCacheTimeMapProvider.class})
        public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory,
                                         RedisCacheConfiguration redisCacheConfiguration,
                                         RedisCacheTimeMapProvider redisCacheTimeMapProvider,
                                         ObjectProvider<RedisSerializer<Object>> serializerProvider) {

            RedisSerializer<Object> redisSerializer = serializerProvider.getIfAvailable(GenericJackson2JsonRedisSerializer::new);

            RedisCacheManager redisCacheManager = new RedisCacheManager(
                    RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory),
                    redisCacheConfiguration,
                    redisCacheTimeMapProvider.cacheConfigurationHashMap(redisSerializer)
            );
            redisCacheManager.setTransactionAware(true);
            return redisCacheManager;
        }
    }
}