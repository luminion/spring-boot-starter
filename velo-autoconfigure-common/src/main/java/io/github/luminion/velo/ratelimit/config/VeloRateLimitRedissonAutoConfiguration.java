package io.github.luminion.velo.ratelimit.config;

import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.ratelimit.support.RedissonRateLimitHandler;
import org.aspectj.weaver.Advice;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 限流自动配置 (Redisson 实现)
 *
 * @author luminion
 * @since 1.0.0
 */
@AutoConfiguration(afterName = { "org.redisson.spring.starter.RedissonAutoConfiguration" })
@ConditionalOnClass({ Advice.class, RedissonClient.class })
@ConditionalOnProperty(prefix = "velo.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloRateLimitRedissonAutoConfiguration {

    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(RateLimitHandler.class)
    @ConditionalOnProperty(prefix = "velo.rate-limit.backends", name = "redisson-enabled", havingValue = "true", matchIfMissing = true)
    public RateLimitHandler rateLimitHandler(RedissonClient redissonClient) {
        return new RedissonRateLimitHandler(redissonClient);
    }

}
