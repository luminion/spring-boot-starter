package io.github.luminion.velo.lock.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot 4 锁 Redis 自动配置入口。
 *
 * @author luminion
 * @since 1.3.1
 */
@AutoConfiguration(
        after = VeloLockRedissonAutoConfiguration.class,
        afterName = "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration"
)
@Import(VeloLockRedisConfiguration.class)
public class VeloLockRedisAutoConfiguration {
}
