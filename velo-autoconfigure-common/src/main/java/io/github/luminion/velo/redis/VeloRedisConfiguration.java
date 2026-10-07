package io.github.luminion.velo.redis;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Bean 配置实现，由各 Spring Boot 版本适配模块的自动配置入口导入。
 *
 * @author luminion
 * @since 1.3.1
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = {
        "org.springframework.data.redis.core.RedisOperations",
        "org.springframework.data.redis.connection.RedisConnectionFactory"
})
@ConditionalOnProperty(prefix = "velo.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloRedisConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "stringObjectRedisTemplate")
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    public RedisTemplate<String, Object> stringObjectRedisTemplate(RedisConnectionFactory redisConnectionFactory,
            ObjectProvider<RedisSerializer<Object>> redisSerializerProvider,
            ObjectProvider<RedisJsonSerializerFactory> jsonFactoryProvider) {
        // 原生泛型注入仅选择通用 Object serializer；多个候选须用 @Primary / @Qualifier 消除歧义。
        RedisSerializer<Object> redisSerializer = redisSerializerProvider.getIfAvailable(
                () -> jsonFactoryProvider.getObject().create(Object.class));
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(redisSerializer);
        redisTemplate.setHashValueSerializer(redisSerializer);
        redisTemplate.setEnableTransactionSupport(false);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory,
            ObjectProvider<RedisSerializer<Object>> redisSerializerProvider,
            ObjectProvider<RedisJsonSerializerFactory> jsonFactoryProvider) {
        RedisSerializer<Object> redisSerializer = redisSerializerProvider.getIfAvailable(
                () -> jsonFactoryProvider.getObject().create(Object.class));
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setDefaultSerializer(redisSerializer);
        redisTemplate.setKeySerializer(StringRedisSerializer.UTF_8);
        redisTemplate.setHashKeySerializer(StringRedisSerializer.UTF_8);
        redisTemplate.setValueSerializer(redisSerializer);
        redisTemplate.setHashValueSerializer(redisSerializer);
        redisTemplate.setEnableTransactionSupport(false);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
