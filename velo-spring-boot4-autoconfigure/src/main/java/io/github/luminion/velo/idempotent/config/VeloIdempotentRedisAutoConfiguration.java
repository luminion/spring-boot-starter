package io.github.luminion.velo.idempotent.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot 4 幂等 Redis 自动配置入口。
 *
 * @author luminion
 * @since 1.3.1
 */
@AutoConfiguration(
        after = VeloIdempotentRedissonAutoConfiguration.class,
        afterName = "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration"
)
@Import(VeloIdempotentRedisConfiguration.class)
public class VeloIdempotentRedisAutoConfiguration {
}
