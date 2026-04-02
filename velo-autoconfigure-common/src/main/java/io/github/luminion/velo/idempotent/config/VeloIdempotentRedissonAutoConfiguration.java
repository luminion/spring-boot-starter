package io.github.luminion.velo.idempotent.config;

import io.github.luminion.velo.idempotent.IdempotentHandler;
import io.github.luminion.velo.idempotent.support.RedissonIdempotentHandler;
import org.aspectj.weaver.Advice;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(prefix = "velo.idempotent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloIdempotentRedissonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdempotentHandler.class)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "velo.idempotent.backends", name = "redisson-enabled", havingValue = "true", matchIfMissing = true)
    public IdempotentHandler idempotentHandler(RedissonClient redissonClient) {
        return new RedissonIdempotentHandler(redissonClient);
    }

}
