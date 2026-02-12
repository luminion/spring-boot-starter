package io.github.luminion.starter.autoconfig;

import io.github.luminion.starter.support.cache.RedisCacheTimeMapProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * spring缓存自动配置
 *
 * @author luminion
 * @see org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration
 * @since 1.0.0
 */
@AutoConfiguration(after = {CouchbaseDataAutoConfiguration.class, HazelcastAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class, RedisAutoConfiguration.class})
@ConditionalOnClass(CacheManager.class)
@ConditionalOnBean(CacheAspectSupport.class)
@ConditionalOnMissingBean(value = CacheManager.class, name = "cacheResolver")
public class CacheAutoConfiguration {


    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({RedisCacheManager.class, RedisConnectionFactory.class, RedisSerializer.class})
    static class RedisCacheManagerConfiguration {

        /**
         * Redis缓存时间映射提供程序
         *
         * @return redis缓存时间映射提供程序
         */
        @Bean
        @ConditionalOnMissingBean(RedisCacheTimeMapProvider.class)
        public RedisCacheTimeMapProvider redisCacheTimeMapProvider() {
            return new RedisCacheTimeMapProvider(new HashMap<>());
        }
        

        /**
         * 默认redis缓存配置
         *
         * @param redisSerializer redis序列化程序
         * @return redis缓存配置
         */
        @Bean
        @ConditionalOnMissingBean(RedisCacheConfiguration.class)
        @ConditionalOnBean(RedisSerializer.class)
        private RedisCacheConfiguration redisCacheConfiguration(RedisSerializer<Object> redisSerializer) {
            RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig();
            // 设置序列化器
            RedisSerializationContext.SerializationPair<Object> objectSerializationPair = RedisSerializationContext
                    .SerializationPair
                    .fromSerializer(redisSerializer);
            // 设置缓存过期时间
            redisCacheConfiguration = redisCacheConfiguration
                    .serializeValuesWith(objectSerializationPair)
                    .computePrefixWith(cacheName -> cacheName)
                    .entryTtl(Duration.ofSeconds(3000));
            return redisCacheConfiguration;
        }

        /**
         * redis缓存管理器
         *
         * @param redisConnectionFactory  redis连接工厂
         * @param redisCacheConfiguration redis缓存配置
         * @return 缓存管理器
         */
        @Bean
        @ConditionalOnMissingBean(CacheManager.class)
        @ConditionalOnBean({RedisConnectionFactory.class, RedisCacheConfiguration.class, RedisCacheTimeMapProvider.class, RedisSerializer.class})
        public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory,
                                         RedisCacheConfiguration redisCacheConfiguration,
                                         RedisCacheTimeMapProvider redisCacheTimeMapProvider,
                                         RedisSerializer<Object> redisSerializer
        ) {
            RedisCacheManager redisCacheManager = new RedisCacheManager(
                    RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory),
                    // 默认策略，未配置的 key 会使用这个
                    redisCacheConfiguration,
                    // 指定 key 策略
                    redisCacheTimeMapProvider.cacheConfigurationHashMap(redisSerializer)
            );
            redisCacheManager.setTransactionAware(true);
            return redisCacheManager;
        }
        
    }

}
