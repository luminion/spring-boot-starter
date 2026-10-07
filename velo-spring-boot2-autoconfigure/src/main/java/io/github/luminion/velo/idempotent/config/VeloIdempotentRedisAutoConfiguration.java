package io.github.luminion.velo.idempotent.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot 2 幂等 Redis 自动配置入口。
 *
 * @author luminion
 * @since 1.3.1
 */
@AutoConfiguration(after = {RedisAutoConfiguration.class, VeloIdempotentRedissonAutoConfiguration.class})
@Import(VeloIdempotentRedisConfiguration.class)
public class VeloIdempotentRedisAutoConfiguration {
}
