package io.github.luminion.autoconfigure.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
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
    //@ConditionalOnBean(Jackson2ObjectMapperBuilder.class)
    //public RedisSerializer<Object> redisSerializer(Jackson2ObjectMapperBuilder builder) {
    //    log.debug("RedisSerializer Configured");
    //    ObjectMapper objectMapper = builder.build();
    //    //启用反序列化所需的类型信息,在属性中添加@class
    //    objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
    //    //配置null值的序列化器
    //    GenericJackson2JsonRedisSerializer.registerNullValueSerializer(objectMapper, null);
    //    return new GenericJackson2JsonRedisSerializer(objectMapper);
    //}

    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    @ConditionalOnBean({RedisConnectionFactory.class, RedisSerializer.class})
    @ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory, RedisSerializer<Object> redisSerializer) {
        log.debug("RedisTemplate Configured");
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setDefaultSerializer(redisSerializer);
        template.setValueSerializer(redisSerializer);
        template.setHashValueSerializer(redisSerializer);
        template.setKeySerializer(StringRedisSerializer.UTF_8);
        template.setHashKeySerializer(StringRedisSerializer.UTF_8);
        template.afterPropertiesSet();
        return template;
    }
}