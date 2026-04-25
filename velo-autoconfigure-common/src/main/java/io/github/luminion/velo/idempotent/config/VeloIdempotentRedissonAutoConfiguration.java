package io.github.luminion.velo.idempotent.config;

import io.github.luminion.velo.core.ConcurrencyBackend;
import io.github.luminion.velo.core.condition.ConditionalOnConcurrencyBackend;
import io.github.luminion.velo.idempotent.IdempotentHandler;
import io.github.luminion.velo.idempotent.support.RedissonIdempotentHandler;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
@ConditionalOnConcurrencyBackend(prefix = "velo.idempotent", value = ConcurrencyBackend.REDISSON,
        autoClassNames = {"org.aspectj.weaver.Advice", "org.redisson.api.RedissonClient"})
@ConditionalOnMissingBean(IdempotentHandler.class)
@ConditionalOnProperty(prefix = "velo.idempotent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloIdempotentRedissonAutoConfiguration {

    @Bean
    @ConditionalOnConcurrencyBackend(prefix = "velo.idempotent", value = ConcurrencyBackend.REDISSON,
            autoBeanTypeNames = "org.redisson.api.RedissonClient")
    @ConditionalOnMissingBean(IdempotentHandler.class)
    public IdempotentHandler idempotentHandler(RedissonClient redissonClient) {
        return new RedissonIdempotentHandler(redissonClient);
    }

}
