package io.github.luminion.starter.idempotent.config;

import io.github.luminion.starter.idempotent.IdempotentHandler;
import io.github.luminion.starter.idempotent.support.RedissonIdempotentHandler;
import org.aspectj.weaver.Advice;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 幂等自动配置 (Redisson 实现)
 *
 * @author luminion
 * @see org.redisson.spring.starter.RedissonAutoConfiguration
 * @since 1.0.0
 */
@AutoConfiguration(afterName = {"org.redisson.spring.starter.RedissonAutoConfiguration"})
@ConditionalOnClass({Advice.class, RedissonClient.class})
public class LuminionIdempotentRedissonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdempotentHandler.class)
    @ConditionalOnBean(RedissonClient.class)
    public IdempotentHandler idempotentHandler(RedissonClient redissonClient) {
        return new RedissonIdempotentHandler(redissonClient);
    }

}
