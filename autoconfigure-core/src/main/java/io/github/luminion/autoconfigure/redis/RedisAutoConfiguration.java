package io.github.luminion.autoconfigure.redis;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
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
@Slf4j
@AutoConfiguration
@ConditionalOnClass(RedisOperations.class)
@ConditionalOnProperty(value = "luminion.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisAutoConfiguration {


    //@Bean
    //@ConditionalOnMissingBean(name = "redisTemplate")
    //@ConditionalOnBean({RedisConnectionFactory.class, RedisSerializer.class})
    //@ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    //public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory, RedisSerializer<Object> redisSerializer) {
    //    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    //    redisTemplate.setConnectionFactory(redisConnectionFactory);
    //    redisTemplate.setKeySerializer(new StringRedisSerializer());
    //    redisTemplate.setHashKeySerializer(new StringRedisSerializer());
    //    redisTemplate.setValueSerializer(redisSerializer);
    //    redisTemplate.setHashValueSerializer(redisSerializer);
    //    redisTemplate.setEnableTransactionSupport(false);
    //
    //    redisTemplate.afterPropertiesSet();
    //    return redisTemplate;
    //}

    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    @ConditionalOnBean({RedisConnectionFactory.class, RedisSerializer.class})
    @ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory, RedisSerializer<Object> redisSerializer) {
        log.debug("RedisTemplate Configured");
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