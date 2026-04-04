package io.github.luminion.velo.lock.config;

import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.lock.support.CaffeineLockHandler;
import org.aspectj.weaver.Advice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 锁自动配置 (Caffeine 实现)
 */
@AutoConfiguration(after = VeloLockRedisAutoConfiguration.class)
@ConditionalOnClass({Advice.class, com.github.benmanes.caffeine.cache.Cache.class})
@ConditionalOnProperty(prefix = "velo.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloLockCaffeineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LockHandler.class)
    @ConditionalOnProperty(prefix = "velo.lock.backends", name = "caffeine-enabled", havingValue = "true", matchIfMissing = true)
    public LockHandler lockHandler() {
        return new CaffeineLockHandler();
    }
}
