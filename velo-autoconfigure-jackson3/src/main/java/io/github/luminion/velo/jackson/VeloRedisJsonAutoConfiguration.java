package io.github.luminion.velo.jackson;

import io.github.luminion.velo.redis.RedisJsonSerializerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Type;

/**
 * Jackson 3 的 Redis JSON 适配，与 HTTP 输出规则隔离。
 */
@AutoConfiguration
@ConditionalOnClass({JsonMapper.class, RedisSerializer.class})
public class VeloRedisJsonAutoConfiguration {

    @Bean
    @Lazy
    @ConditionalOnMissingBean(RedisJsonSerializerFactory.class)
    public RedisJsonSerializerFactory redisJsonSerializerFactory() {
        JsonMapper mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        RedisSerializer<Object> generic = RedisSerializer.json();
        return new RedisJsonSerializerFactory() {
            @Override
            public RedisSerializer<Object> create(Type type) {
                JavaType javaType = mapper.constructType(type);
                return new JacksonJsonRedisSerializer<>(mapper, javaType);
            }

            @Override
            public RedisSerializer<Object> genericCacheSerializer() {
                return generic;
            }
        };
    }
}
