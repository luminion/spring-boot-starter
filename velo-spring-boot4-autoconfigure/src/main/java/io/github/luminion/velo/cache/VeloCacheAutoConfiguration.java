package io.github.luminion.velo.cache;

import io.github.luminion.velo.VeloProperties;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.cache.autoconfigure.CacheProperties;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.BatchStrategy;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * Spring Boot 4 缓存增强，保留原生管理器创建与自定义流程。
 *
 * @author luminion
 * @since 1.3.1
 */
@AutoConfiguration(
        afterName = {
                "org.springframework.boot.data.couchbase.autoconfigure.DataCouchbaseAutoConfiguration",
                "org.springframework.boot.hazelcast.autoconfigure.HazelcastAutoConfiguration",
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
                "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration"
        },
        beforeName = "org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration"
)
@ConditionalOnProperty(prefix = "velo.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloCacheAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({RedisCacheManager.class, RedisCacheManagerBuilderCustomizer.class})
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnMissingBean(value = CacheManager.class, name = "cacheResolver")
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    @EnableConfigurationProperties(CacheProperties.class)
    static class RedisConfiguration {

        @Bean
        @ConditionalOnMissingBean(RedisCacheConfiguration.class)
        RedisCacheConfiguration redisCacheConfiguration(CacheProperties springProperties, VeloProperties properties,
                ObjectProvider<RedisSerializer<Object>> serializerProvider) {
            CacheProperties.Redis redis = springProperties.getRedis();
            Duration ttl = redis.getTimeToLive() == null ? VeloCacheConfiguration.DEFAULT_TTL : redis.getTimeToLive();
            VeloCacheConfiguration.validateTtl(ttl, "spring.cache.redis.time-to-live");
            RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig().entryTtl(ttl);
            if (!redis.isCacheNullValues()) {
                defaults = defaults.disableCachingNullValues();
            }
            if (!redis.isUseKeyPrefix()) {
                defaults = defaults.disableKeyPrefix();
            }
            return VeloCacheConfiguration.customizeDefaults(defaults, serializerProvider, redis.getKeyPrefix(), properties);
        }

        @Bean
        @ConditionalOnMissingBean(RedisCacheTimeMapProvider.class)
        RedisCacheTimeMapProvider redisCacheTimeMapProvider(VeloProperties properties) {
            return new RedisCacheTimeMapProvider(properties.getCache().getTtl());
        }

        @Bean
        @ConditionalOnMissingBean(RedisCacheWriter.class)
        RedisCacheWriter veloRedisCacheWriter(RedisConnectionFactory factory) {
            // Spring Data Redis 4 默认异步写入；使用原生开关保证三套版本的写入和清空语义一致。
            BatchStrategy strategy = VeloCacheConfiguration.batchStrategy(factory);
            return RedisCacheWriter.create(factory,
                    configurer -> configurer.immediateWrites().batchStrategy(strategy));
        }

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        RedisCacheManagerBuilderCustomizer veloRedisCacheManagerBuilderCustomizer(RedisCacheWriter writer,
                RedisCacheConfiguration defaults, RedisCacheTimeMapProvider timeMapProvider, VeloProperties properties) {
            return builder -> VeloCacheConfiguration.customizeBuilder(builder, writer, defaults, timeMapProvider, properties);
        }
    }
}
