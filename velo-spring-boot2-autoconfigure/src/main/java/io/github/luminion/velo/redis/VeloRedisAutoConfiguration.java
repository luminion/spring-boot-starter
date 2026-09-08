package io.github.luminion.velo.redis;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot 2 Redis 自动配置入口。
 *
 * @author luminion
 * @since 1.3.1
 */
@AutoConfiguration(before = RedisAutoConfiguration.class)
@Import({VeloBoot2RedisConnectionConfigurationImportSelector.class, VeloRedisConfiguration.class})
public class VeloRedisAutoConfiguration {
}
