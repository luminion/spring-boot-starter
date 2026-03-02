package io.github.luminion.starter.ratelimit.config;

import io.github.luminion.starter.ratelimit.RateLimitHandler;
import io.github.luminion.starter.ratelimit.support.RedisRateLimitHandler;
import org.aspectj.weaver.Advice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 限流自动配置 (Redis 实现)
 *
 * @author luminion
 * @since 1.0.0
 */
@AutoConfiguration(after = { RedisAutoConfiguration.class, LuminionRateLimitRedissonAutoConfiguration.class })
@ConditionalOnClass({ Advice.class, RedisTemplate.class })
public class LuminionRateLimitRedisAutoConfiguration {

    @Bean
    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnBean(RedisTemplate.class)
    @ConditionalOnMissingBean(RateLimitHandler.class)
    public RateLimitHandler rateLimitHandler(RedisTemplate<Object, Object> redisTemplate) {
        return new RedisRateLimitHandler(redisTemplate);
    }

}
