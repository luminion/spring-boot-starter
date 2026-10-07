package io.github.luminion.velo.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.luminion.velo.redis.RedisJsonSerializerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.lang.reflect.Type;

/**
 * Jackson 2 的 Redis JSON 适配，与 HTTP Jackson 增强开关和 mapper 隔离。
 */
@AutoConfiguration
@ConditionalOnClass({ObjectMapper.class, RedisSerializer.class})
public class VeloRedisJsonAutoConfiguration {

    @Bean
    @Lazy
    @ConditionalOnMissingBean(RedisJsonSerializerFactory.class)
    public RedisJsonSerializerFactory redisJsonSerializerFactory() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.registerModule(new JavaTimeModule());
        ObjectMapper genericMapper = mapper.copy();
        genericMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);
        GenericJackson2JsonRedisSerializer.registerNullValueSerializer(genericMapper, null);
        RedisSerializer<Object> generic = new GenericJackson2JsonRedisSerializer(genericMapper);
        return new RedisJsonSerializerFactory() {
            @Override
            public RedisSerializer<Object> create(Type type) {
                // 使用 Boot 2 / Spring Data 2 也提供的构造方式。
                JavaType javaType = mapper.constructType(type);
                Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(javaType);
                serializer.setObjectMapper(mapper);
                return serializer;
            }

            @Override
            public RedisSerializer<Object> genericCacheSerializer() {
                return generic;
            }
        };
    }
}
