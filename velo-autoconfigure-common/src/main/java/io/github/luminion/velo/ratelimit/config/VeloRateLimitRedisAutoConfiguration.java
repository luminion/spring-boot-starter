package io.github.luminion.velo.ratelimit.config;

import io.github.luminion.velo.core.ConcurrencyBackend;
import io.github.luminion.velo.core.condition.ConditionalOnConcurrencyBackend;
import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.ratelimit.support.RedisRateLimitHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
@ConditionalOnConcurrencyBackend(prefix = "velo.rate-limit", value = ConcurrencyBackend.REDIS,
        autoClassNames = {"org.aspectj.weaver.Advice", "org.springframework.data.redis.core.RedisTemplate"})
@ConditionalOnMissingBean(RateLimitHandler.class)
@ConditionalOnProperty(prefix = "velo.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloRateLimitRedisAutoConfiguration {

    @Bean
    @ConditionalOnConcurrencyBackend(prefix = "velo.rate-limit", value = ConcurrencyBackend.REDIS,
            autoBeanNames = "redisTemplate")
    @ConditionalOnMissingBean(RateLimitHandler.class)
    public RateLimitHandler rateLimitHandler(@Qualifier("redisTemplate") RedisTemplate<Object, Object> redisTemplate) {
        return new RedisRateLimitHandler(redisTemplate);
    }

}
