package io.github.luminion.velo.cache;

import io.github.luminion.velo.VeloProperties;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.couchbase.CouchbaseDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.cache.BatchStrategy;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Spring Boot 3 缓存增强，保留原生管理器创建与自定义流程。
 *
 * @author luminion
 * @since 1.3.1
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
@ConditionalOnProperty(prefix = "velo.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloCacheAutoConfiguration {

    /** 在 Redis 缓存类型启用时装配 Velo 的缓存默认配置。 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({RedisCacheManager.class, RedisCacheManagerBuilderCustomizer.class})
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnMissingBean(value = CacheManager.class, name = "cacheResolver")
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    @EnableConfigurationProperties(CacheProperties.class)
    static class RedisConfiguration {

        /**
         * 根据 Spring Boot 属性和 Velo 配置构建 Redis 缓存默认项。
         *
         * @param springProperties Spring Boot 缓存属性
         * @param properties Velo 配置属性
         * @param serializerProvider 可选的 Redis 序列化器
         * @return Redis 缓存默认配置
         */
        @Bean
        @ConditionalOnMissingBean(RedisCacheConfiguration.class)
        RedisCacheConfiguration redisCacheConfiguration(CacheProperties springProperties, VeloProperties properties,
                ObjectProvider<RedisSerializer<Object>> serializerProvider) {
            CacheProperties.Redis redis = springProperties.getRedis();
            Duration ttl = redis.getTimeToLive() == null
                    ? VeloCacheConfiguration.DEFAULT_TTL
                    : redis.getTimeToLive();
            VeloCacheConfiguration.validateTtl(ttl, "spring.cache.redis.time-to-live");
            RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig().entryTtl(ttl);
            if (!redis.isCacheNullValues()) {
                defaults = defaults.disableCachingNullValues();
            }
            if (!redis.isUseKeyPrefix()) {
                defaults = defaults.disableKeyPrefix();
            }
            return VeloCacheConfiguration.customizeDefaults(
                    defaults, serializerProvider, redis.getKeyPrefix(), properties);
        }

        /**
         * 提供按缓存名称解析 TTL 的配置。
         *
         * @param properties Velo 配置属性
         * @return Redis 缓存 TTL 映射提供器
         */
        @Bean
        @ConditionalOnMissingBean(RedisCacheTimeMapProvider.class)
        RedisCacheTimeMapProvider redisCacheTimeMapProvider(VeloProperties properties) {
            return new RedisCacheTimeMapProvider(properties.getCache().getTtl());
        }

        /**
         * 创建使用 Redis 批处理策略的非锁定缓存写入器。
         *
         * @param factory Redis 连接工厂
         * @return Redis 缓存写入器
         */
        @Bean
        @ConditionalOnMissingBean(RedisCacheWriter.class)
        RedisCacheWriter veloRedisCacheWriter(RedisConnectionFactory factory) {
            BatchStrategy strategy = VeloCacheConfiguration.batchStrategy(factory);
            return RedisCacheWriter.nonLockingRedisCacheWriter(factory, strategy);
        }

        /**
         * 将 Velo 的写入器、默认配置和 TTL 映射应用到缓存管理器构建器。
         *
         * @param writer Redis 缓存写入器
         * @param defaults 缓存默认配置
         * @param timeMapProvider 按名称解析 TTL 的提供器
         * @param properties Velo 配置属性
         * @return 缓存管理器构建器定制器
         */
        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        RedisCacheManagerBuilderCustomizer veloRedisCacheManagerBuilderCustomizer(RedisCacheWriter writer,
                RedisCacheConfiguration defaults, RedisCacheTimeMapProvider timeMapProvider,
                VeloProperties properties) {
            return builder -> VeloCacheConfiguration.customizeBuilder(
                    builder, writer, defaults, timeMapProvider, properties);
        }
    }
}
