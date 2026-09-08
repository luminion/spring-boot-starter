package io.github.luminion.velo.ratelimit.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot 4 限流 Redis 自动配置入口。
 *
 * @author luminion
 * @since 1.3.1
 */
@AutoConfiguration(
        after = VeloRateLimitRedissonAutoConfiguration.class,
        afterName = "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration"
)
@Import(VeloRateLimitRedisConfiguration.class)
public class VeloRateLimitRedisAutoConfiguration {
}
