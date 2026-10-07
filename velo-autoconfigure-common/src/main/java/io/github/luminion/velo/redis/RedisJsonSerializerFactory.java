package io.github.luminion.velo.redis;

import org.springframework.data.redis.serializer.RedisSerializer;

import java.lang.reflect.Type;

/**
 * 隔离 Jackson 版本的原生 Redis JSON 序列化器工厂。
 * 默认实现的 mapper 独立于 HTTP 输出规则，不缓存请求数据。
 */
public interface RedisJsonSerializerFactory {

    /**
     * 创建无自动 Java 类型标识的定向序列化器；Object 目标按普通 JSON 读取为 Map/List/标量。
     */
    RedisSerializer<Object> create(Type type);

    /**
     * 未启用按缓存名登记类型时，提供可还原任意常见 DTO 的通用缓存序列化器。
     */
    RedisSerializer<Object> genericCacheSerializer();
}
