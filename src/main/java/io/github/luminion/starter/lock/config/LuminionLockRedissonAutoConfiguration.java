package io.github.luminion.starter.lock.config;

import io.github.luminion.starter.lock.LockHandler;
import io.github.luminion.starter.lock.support.RedissonLockHandler;
import org.aspectj.weaver.Advice;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 锁自动配置 (Redisson 实现)
 *
 * @author luminion
 * @since 1.0.1
 */
@AutoConfiguration(afterName = { "org.redisson.spring.starter.RedissonAutoConfiguration" })
@ConditionalOnClass({ Advice.class, RedissonClient.class })
public class LuminionLockRedissonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LockHandler.class)
    @ConditionalOnBean(RedissonClient.class)
    public LockHandler lockHandler(RedissonClient redissonClient) {
        return new RedissonLockHandler(redissonClient);
    }

}
