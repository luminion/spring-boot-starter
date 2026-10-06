package io.github.luminion.velo.ratelimit.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot 3 Redis 固定一秒窗口限流自动配置入口。
 */
@AutoConfiguration(after = {RedisAutoConfiguration.class, VeloRateLimitRedissonAutoConfiguration.class})
@Import(VeloRateLimitRedisConfiguration.class)
public class VeloRateLimitRedisAutoConfiguration {
}
