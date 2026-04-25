package io.github.luminion.velo.lock.config;

import io.github.luminion.velo.core.ConcurrencyBackend;
import io.github.luminion.velo.core.condition.ConditionalOnConcurrencyBackend;
import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.lock.support.RedisLockHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
@ConditionalOnConcurrencyBackend(prefix = "velo.lock", value = ConcurrencyBackend.REDIS,
        autoClassNames = {"org.aspectj.weaver.Advice", "org.springframework.data.redis.core.StringRedisTemplate"})
@ConditionalOnMissingBean(LockHandler.class)
@ConditionalOnProperty(prefix = "velo.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloLockRedisAutoConfiguration {

    @Bean
    @ConditionalOnConcurrencyBackend(prefix = "velo.lock", value = ConcurrencyBackend.REDIS,
            autoBeanNames = "stringRedisTemplate")
    @ConditionalOnMissingBean(LockHandler.class)
    public LockHandler lockHandler(StringRedisTemplate stringRedisTemplate) {
        return new RedisLockHandler(stringRedisTemplate);
    }

}
