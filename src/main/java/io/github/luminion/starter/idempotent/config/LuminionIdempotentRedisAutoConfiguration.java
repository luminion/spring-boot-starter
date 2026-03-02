package io.github.luminion.starter.idempotent.config;

import io.github.luminion.starter.idempotent.IdempotentHandler;
import io.github.luminion.starter.idempotent.support.RedisIdempotentHandler;
import org.aspectj.weaver.Advice;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 幂等自动配置 (Redis 实现)
 *
 * @author luminion
 * @since 1.0.0
 */
@AutoConfiguration(after = {RedisAutoConfiguration.class, LuminionIdempotentRedissonAutoConfiguration.class})
@ConditionalOnClass({Advice.class, RedissonClient.class})
public class LuminionIdempotentRedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdempotentHandler.class)
    public IdempotentHandler idempotentHandler(RedisTemplate<Object, Object> redisTemplate) {
        return new RedisIdempotentHandler(redisTemplate);
    }

}
