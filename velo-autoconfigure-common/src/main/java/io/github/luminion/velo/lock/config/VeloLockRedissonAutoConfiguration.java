package io.github.luminion.velo.lock.config;

import io.github.luminion.velo.ConcurrencyBackend;
import io.github.luminion.velo.condition.ConditionalOnConcurrencyBackend;
import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.lock.support.RedissonLockHandler;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 锁自动配置 (Redisson 实现)
 *
 * @author luminion
 * @since 1.0.0
 */
@AutoConfiguration(afterName = { "org.redisson.spring.starter.RedissonAutoConfiguration" })
@ConditionalOnConcurrencyBackend(prefix = "velo.lock", value = ConcurrencyBackend.REDISSON,
        autoClassNames = {"org.aspectj.weaver.Advice", "org.redisson.api.RedissonClient"})
@ConditionalOnMissingBean(LockHandler.class)
@ConditionalOnProperty(prefix = "velo.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloLockRedissonAutoConfiguration {

    @Bean
    @ConditionalOnConcurrencyBackend(prefix = "velo.lock", value = ConcurrencyBackend.REDISSON,
            autoBeanTypeNames = "org.redisson.api.RedissonClient")
    @ConditionalOnMissingBean(LockHandler.class)
    public LockHandler lockHandler(RedissonClient redissonClient) {
        return new RedissonLockHandler(redissonClient);
    }

}
