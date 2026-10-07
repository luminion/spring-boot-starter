package io.github.luminion.velo.cache;

import io.github.luminion.velo.VeloProperties;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.cache.BatchStrategy;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager.RedisCacheManagerBuilder;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * 各 Boot 版本共用的缓存增强；管理器创建和用户自定义器执行由 Boot 原生流程负责。
 */
public final class VeloCacheConfiguration {

    public static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private VeloCacheConfiguration() {
    }

    /**
     * 在 Spring 通用设置之上应用默认 JSON 序列化和单冒号分隔格式。
     * 用户提供完整 RedisCacheConfiguration Bean 时，各版本适配器会跳过此方法。
     */
    public static RedisCacheConfiguration customizeDefaults(RedisCacheConfiguration defaults,
                                                            ObjectProvider<RedisSerializer<Object>> serializerProvider, String keyPrefix,
                                                            VeloProperties properties) {
        // 候选不明确时沿用 Spring 的异常，避免悄然切换缓存读写格式。
        RedisSerializer<Object> serializer = serializerProvider.getIfAvailable(RedisSerializer::json);
        RedisCacheConfiguration configuration = defaults.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer));
        if (configuration.usePrefix()) {
            String configuredSeparator = properties.getCache().getSeparator();
            String separator = StringUtils.hasText(configuredSeparator) ? configuredSeparator : ":";
            String prefix = keyPrefix == null ? "" : keyPrefix;
            configuration = configuration.computePrefixWith(cacheName -> prefix + cacheName + separator);
        }
        return configuration;
    }

    /**
     * Lettuce 使用原生 SCAN 批处理，其他连接工厂保留原生 KEYS 策略。
     */
    public static BatchStrategy batchStrategy(RedisConnectionFactory connectionFactory) {
        boolean lettuceAvailable = ClassUtils.isPresent("io.lettuce.core.RedisClient",
                VeloCacheConfiguration.class.getClassLoader());
        if (lettuceAvailable && connectionFactory instanceof LettuceConnectionFactory) {
            return BatchStrategies.scan(1000);
        }
        return BatchStrategies.keys();
    }

    /**
     * 只设置 Velo 默认增强；后续用户自定义器可以覆盖这些设置。
     * 各版本适配器负责选用原生 writer 的同步写入设置。
     */
    public static void customizeBuilder(RedisCacheManagerBuilder builder, RedisCacheWriter delegate,
                                        RedisCacheConfiguration defaults, RedisCacheTimeMapProvider timeMapProvider, VeloProperties properties) {
        RedisCacheWriter writer = JitterRedisCacheWriter.wrap(delegate, properties.getCache().getTtlJitterPercentage());
        Map<String, RedisCacheConfiguration> initialConfigurations = timeMapProvider.cacheConfigurationHashMap(defaults);
        builder.cacheWriter(writer).withInitialCacheConfigurations(initialConfigurations);
        if (properties.getCache().isTransactionAware()) {
            builder.transactionAware();
        }
    }

    /**
     * 仅零表示不过期；正 TTL 至少为一毫秒，且必须能表示为底层使用的 long 毫秒数。
     */
    public static Duration validateTtl(Duration ttl, String property) {
        if (ttl == null || ttl.isNegative() || (!ttl.isZero() && ttl.compareTo(Duration.ofMillis(1)) < 0)) {
            throw new IllegalArgumentException(property + " 必须为 0（不过期）或至少 1ms 的正值");
        }
        try {
            ttl.toMillis();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(property + " 超出可支持的毫秒范围", ex);
        }
        return ttl;
    }
}
