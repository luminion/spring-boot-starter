package io.github.luminion.starter.lock.config;

import io.github.luminion.starter.lock.LockHandler;
import io.github.luminion.starter.lock.support.RedisLockHandler;
import org.aspectj.weaver.Advice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 锁自动配置 (Redis 实现)
 * 
 * @author luminion
 * @since 1.0.1
 */
@AutoConfiguration(after = { RedisAutoConfiguration.class, LuminionLockRedissonAutoConfiguration.class })
@ConditionalOnClass({ Advice.class, StringRedisTemplate.class })
public class LuminionLockRedisAutoConfiguration {

    @Bean
    @ConditionalOnBean(name = "stringRedisTemplate")
    @ConditionalOnMissingBean(LockHandler.class)
    public LockHandler lockHandler(StringRedisTemplate stringRedisTemplate) {
        return new RedisLockHandler(stringRedisTemplate);
    }

}
