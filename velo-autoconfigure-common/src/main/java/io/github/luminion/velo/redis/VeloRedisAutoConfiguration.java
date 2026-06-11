package io.github.luminion.velo.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis自动配置类
 *
 * @author luminion
 * @see org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
 */
@AutoConfiguration(after = RedisAutoConfiguration.class)
@ConditionalOnClass(RedisOperations.class)
@ConditionalOnProperty(prefix = "velo.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloRedisAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(VeloRedisAutoConfiguration.class);

    //
    //@Bean
    //@ConditionalOnMissingBean
    //@ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    //public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
    //    // 该配置由官方starter提供
    //    return new StringRedisTemplate(redisConnectionFactory);
    //}

    @Bean
    @ConditionalOnMissingBean(name = "stringObjectRedisTemplate")
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    public RedisTemplate<String, Object> stringObjectRedisTemplate(RedisConnectionFactory redisConnectionFactory,
            ObjectProvider<RedisSerializer<Object>> redisSerializerProvider) {
        RedisSerializer<Object> redisSerializer = redisSerializerProvider.getIfAvailable(
                () -> defaultRedisSerializer("stringObjectRedisTemplate"));
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
            ObjectProvider<RedisSerializer<Object>> redisSerializerProvider) {
        RedisSerializer<Object> redisSerializer = redisSerializerProvider.getIfAvailable(
                () -> defaultRedisSerializer("redisTemplate"));
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

    private RedisSerializer<Object> defaultRedisSerializer(String beanName) {
        log.debug("No RedisSerializer bean found, using GenericJackson2JsonRedisSerializer for {}", beanName);
        return new GenericJackson2JsonRedisSerializer();
    }
}
