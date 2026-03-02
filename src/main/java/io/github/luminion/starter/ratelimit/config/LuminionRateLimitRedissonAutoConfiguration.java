package io.github.luminion.starter.ratelimit.config;

import io.github.luminion.starter.ratelimit.RateLimitHandler;
import io.github.luminion.starter.ratelimit.support.RedissonRateLimitHandler;
import org.aspectj.weaver.Advice;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 限流自动配置 (Redisson 实现)
 *
 * @author luminion
 * @since 1.0.1
 */
@AutoConfiguration(afterName = { "org.redisson.spring.starter.RedissonAutoConfiguration" })
@ConditionalOnClass({ Advice.class, RedissonClient.class })
public class LuminionRateLimitRedissonAutoConfiguration {

    @Bean
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(RateLimitHandler.class)
    public RateLimitHandler rateLimitHandler(RedissonClient redissonClient) {
        return new RedissonRateLimitHandler(redissonClient);
    }

}
