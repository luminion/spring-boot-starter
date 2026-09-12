package io.github.luminion.velo.redis;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot 3 Redis 自动配置入口。
 *
 * @author luminion
 * @since 1.3.1
 */
@AutoConfiguration(before = RedisAutoConfiguration.class)
@ConditionalOnClass(name = {
        "org.springframework.data.redis.core.RedisOperations",
        "org.springframework.data.redis.connection.RedisConnectionFactory"
})
@Import({VeloBoot3RedisConnectionConfigurationImportSelector.class, VeloRedisConfiguration.class})
public class VeloRedisAutoConfiguration {
}
