package io.github.luminion.velo.idempotent.config;

import io.github.luminion.velo.idempotent.IdempotentHandler;
import io.github.luminion.velo.idempotent.support.RedisIdempotentHandler;
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
 * 幂等自动配置 (Redis 实现)
 *
 * @author luminion
 * @since 1.0.0
 */
@AutoConfiguration(after = {RedisAutoConfiguration.class, VeloIdempotentRedissonAutoConfiguration.class})
@ConditionalOnClass({Advice.class, RedisTemplate.class})
@ConditionalOnProperty(prefix = "velo.idempotent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloIdempotentRedisAutoConfiguration {

    @Bean
    @ConditionalOnBean(name = "redisTemplate")
    @ConditionalOnMissingBean(IdempotentHandler.class)
    @ConditionalOnProperty(prefix = "velo.idempotent.backends", name = "redis-enabled", havingValue = "true", matchIfMissing = true)
    public IdempotentHandler idempotentHandler(@Qualifier("redisTemplate") RedisTemplate<Object, Object> redisTemplate) {
        return new RedisIdempotentHandler(redisTemplate);
    }

}
