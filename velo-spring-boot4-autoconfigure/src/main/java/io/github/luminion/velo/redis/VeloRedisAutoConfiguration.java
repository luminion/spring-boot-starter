package io.github.luminion.velo.redis;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot 4 Redis 自动配置入口。
 *
 * @author luminion
 * @since 1.3.1
 */
@AutoConfiguration(beforeName = "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration")
@Import({VeloBoot4RedisConnectionConfigurationImportSelector.class, VeloRedisConfiguration.class})
public class VeloRedisAutoConfiguration {
}
