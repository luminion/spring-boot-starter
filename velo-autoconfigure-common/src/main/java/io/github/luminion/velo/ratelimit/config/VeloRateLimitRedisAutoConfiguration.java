package io.github.luminion.velo.ratelimit.config;

import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.ratelimit.support.RedisRateLimitHandler;
import org.aspectj.weaver.Advice;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 限流自动配置 (Redis 实现)
 *
 * @author luminion
 * @since 1.0.0
 */
@AutoConfiguration(after = { RedisAutoConfiguration.class, VeloRateLimitRedissonAutoConfiguration.class })
@ConditionalOnClass({ Advice.class, RedisTemplate.class })
@ConditionalOnProperty(prefix = "velo.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloRateLimitRedisAutoConfiguration {

    @Bean
    @ConditionalOnBean(name = "redisTemplate")
    @ConditionalOnMissingBean(RateLimitHandler.class)
    @ConditionalOnProperty(prefix = "velo.rate-limit.backends", name = "redis-enabled", havingValue = "true", matchIfMissing = true)
    public RateLimitHandler rateLimitHandler(@Qualifier("redisTemplate") RedisTemplate<Object, Object> redisTemplate) {
        return new RedisRateLimitHandler(redisTemplate);
    }

}
