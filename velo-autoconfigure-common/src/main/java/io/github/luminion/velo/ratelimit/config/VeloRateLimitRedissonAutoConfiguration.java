package io.github.luminion.velo.ratelimit.config;

import io.github.luminion.velo.ConcurrencyBackend;
import io.github.luminion.velo.condition.ConditionalOnConcurrencyBackend;
import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.ratelimit.support.RedissonRateLimitHandler;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 限流自动配置 (Redisson 实现)
 *
 * @author luminion
 * @since 1.0.0
 */
@AutoConfiguration(afterName = {"org.redisson.spring.starter.RedissonAutoConfiguration",
        "org.redisson.spring.starter.RedissonAutoConfigurationV2",
        "org.redisson.spring.starter.RedissonAutoConfigurationV4"})
@ConditionalOnClass(name = "org.redisson.api.RedissonClient")
@ConditionalOnConcurrencyBackend(prefix = "velo.rate-limit", backend = ConcurrencyBackend.REDISSON,
        autoClassNames = {"org.aspectj.weaver.Advice", "org.redisson.api.RedissonClient"})
@ConditionalOnMissingBean(RateLimitHandler.class)
@ConditionalOnProperty(prefix = "velo.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloRateLimitRedissonAutoConfiguration {

    @Bean
    @ConditionalOnConcurrencyBackend(prefix = "velo.rate-limit", backend = ConcurrencyBackend.REDISSON,
            autoBeanTypeNames = "org.redisson.api.RedissonClient")
    @ConditionalOnMissingBean(RateLimitHandler.class)
    public RateLimitHandler rateLimitHandler(RedissonClient redissonClient) {
        return new RedissonRateLimitHandler(redissonClient);
    }

}
