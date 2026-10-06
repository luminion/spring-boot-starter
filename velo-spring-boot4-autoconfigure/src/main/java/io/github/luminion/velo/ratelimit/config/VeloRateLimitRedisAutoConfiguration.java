package io.github.luminion.velo.ratelimit.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot 4 Redis 固定一秒窗口限流自动配置入口。
 */
@AutoConfiguration(after = VeloRateLimitRedissonAutoConfiguration.class,
        afterName = "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration")
@Import(VeloRateLimitRedisConfiguration.class)
public class VeloRateLimitRedisAutoConfiguration {
}
