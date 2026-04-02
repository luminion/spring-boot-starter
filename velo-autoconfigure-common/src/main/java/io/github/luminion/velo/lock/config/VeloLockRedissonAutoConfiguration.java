package io.github.luminion.velo.lock.config;

import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.lock.support.RedissonLockHandler;
import org.aspectj.weaver.Advice;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
@ConditionalOnClass({ Advice.class, RedissonClient.class })
@ConditionalOnProperty(prefix = "velo.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloLockRedissonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LockHandler.class)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnProperty(prefix = "velo.lock.backends", name = "redisson-enabled", havingValue = "true", matchIfMissing = true)
    public LockHandler lockHandler(RedissonClient redissonClient) {
        return new RedissonLockHandler(redissonClient);
    }

}
