package io.github.luminion.velo.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.luminion.velo.condition.OnMissingRedisObjectSerializerCondition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Jackson 2 的 Redis JSON 适配，与 HTTP Jackson 增强开关和 mapper 隔离。
 */
@AutoConfiguration(beforeName = {
        "io.github.luminion.velo.redis.VeloRedisAutoConfiguration",
        "io.github.luminion.velo.cache.VeloCacheAutoConfiguration"
})
@ConditionalOnClass({ObjectMapper.class, RedisSerializer.class})
public class VeloRedisJsonAutoConfiguration {

    @Bean
    @Conditional(OnMissingRedisObjectSerializerCondition.class)
    public RedisSerializer<Object> veloRedisSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.registerModule(new JavaTimeModule());
        // 默认信任 Redis 及其内容；通用缓存有意按类型元数据回读 POJO，不额外限定业务类型白名单。
        // 需要限制类型时由业务提供定向 serializer 或自定义 RedisSerializer<Object> Bean。
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        GenericJackson2JsonRedisSerializer.registerNullValueSerializer(mapper, null);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}
