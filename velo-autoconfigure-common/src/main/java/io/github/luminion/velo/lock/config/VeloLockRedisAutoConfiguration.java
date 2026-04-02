package io.github.luminion.velo.lock.config;

import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.lock.support.RedisLockHandler;
import org.aspectj.weaver.Advice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 锁自动配置 (Redis 实现)
 * 
 * @author luminion
 * @since 1.0.0
 */
@AutoConfiguration(after = { RedisAutoConfiguration.class, VeloLockRedissonAutoConfiguration.class })
@ConditionalOnClass({ Advice.class, StringRedisTemplate.class })
@ConditionalOnProperty(prefix = "velo.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloLockRedisAutoConfiguration {

    @Bean
    @ConditionalOnBean(name = "stringRedisTemplate")
    @ConditionalOnMissingBean(LockHandler.class)
    @ConditionalOnProperty(prefix = "velo.lock.backends", name = "redis-enabled", havingValue = "true", matchIfMissing = true)
    public LockHandler lockHandler(StringRedisTemplate stringRedisTemplate) {
        return new RedisLockHandler(stringRedisTemplate);
    }

}
