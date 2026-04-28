package io.github.luminion.velo.lock.config;

import io.github.luminion.velo.ConcurrencyBackend;
import io.github.luminion.velo.condition.ConditionalOnConcurrencyBackend;
import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.lock.support.CaffeineLockHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 锁自动配置 (Caffeine 实现)
 */
@AutoConfiguration(after = VeloLockRedisAutoConfiguration.class)
@ConditionalOnConcurrencyBackend(prefix = "velo.lock", value = ConcurrencyBackend.CAFFEINE,
        autoClassNames = {"org.aspectj.weaver.Advice", "com.github.benmanes.caffeine.cache.Cache"})
@ConditionalOnMissingBean(LockHandler.class)
@ConditionalOnProperty(prefix = "velo.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloLockCaffeineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LockHandler.class)
    public LockHandler lockHandler() {
        return new CaffeineLockHandler();
    }
}
