package io.github.luminion.autoconfigure.cache;

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
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

/**
 * spring缓存自动配置
 *
 * @author luminion
 * @see org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration
 * @since 1.0.0
 */
@AutoConfiguration(
        after = {CouchbaseDataAutoConfiguration.class, HazelcastAutoConfiguration.class, HibernateJpaAutoConfiguration.class, RedisAutoConfiguration.class}
)
@ConditionalOnClass({CacheManager.class})
@ConditionalOnBean({CacheAspectSupport.class})
@ConditionalOnMissingBean(
        value = {CacheManager.class},
        name = {"cacheResolver"}
)
public class CacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager(RedisConnectionFactory factory, RedisSerializer<Object> redisSerializer) {
        RedisCacheManager cacheManager = RedisCacheManager.builder(factory)
                .cacheDefaults(getRedisCacheConfigurationWithTtl(60, redisSerializer))
                .build();
        return cacheManager;
    }

    private RedisCacheConfiguration getRedisCacheConfigurationWithTtl(Integer minutes, RedisSerializer<Object> redisSerializer) {
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig();
        redisCacheConfiguration = redisCacheConfiguration
                .prefixKeysWith("ct:crm:")
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
                .entryTtl(Duration.ofMinutes(minutes));
        return redisCacheConfiguration;
    }

    @Bean
    public KeyGenerator keyGenerator() {
        // 当没有指定缓存的 key时来根据类名、方法名和方法参数来生成key
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getName())
                    .append(':')
                    .append(method.getName());
            if (params.length > 0) {
                sb.append('[');
                for (Object obj : params) {
                    if (obj != null) {
                        sb.append(obj.toString());
                    }
                }
                sb.append(']');
            }
            return sb.toString();
        };
    }

}
