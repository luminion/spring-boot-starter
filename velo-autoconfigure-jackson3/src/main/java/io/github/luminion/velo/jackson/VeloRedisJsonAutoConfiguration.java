package io.github.luminion.velo.jackson;

import io.github.luminion.velo.condition.OnMissingRedisObjectSerializerCondition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson 3 的 Redis JSON 适配，与 HTTP 输出规则隔离。
 */
@AutoConfiguration(beforeName = {
        "io.github.luminion.velo.redis.VeloRedisAutoConfiguration",
        "io.github.luminion.velo.cache.VeloCacheAutoConfiguration"
})
@ConditionalOnClass({JsonMapper.class, RedisSerializer.class})
public class VeloRedisJsonAutoConfiguration {

    @Bean
    @Conditional(OnMissingRedisObjectSerializerCondition.class)
    public RedisSerializer<Object> veloRedisSerializer() {
        // 默认信任 Redis 及其内容，沿用库的通用类型元数据回读，不额外维护业务类型白名单。
        // 需要限制类型时由业务提供定向 serializer 或自定义 RedisSerializer<Object> Bean。
        return RedisSerializer.json();
    }
}
