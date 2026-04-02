package io.github.luminion.velo.lock;

import io.github.luminion.velo.core.VeloProperties;
import io.github.luminion.velo.core.spi.Fingerprinter;
import io.github.luminion.velo.lock.aspect.LockAspect;
import org.aspectj.weaver.Advice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 锁自动配置实现。
 */
@AutoConfiguration(after = {
        io.github.luminion.velo.lock.config.VeloLockRedissonAutoConfiguration.class,
        io.github.luminion.velo.lock.config.VeloLockRedisAutoConfiguration.class,
        io.github.luminion.velo.lock.config.VeloLockJdkAutoConfiguration.class
})
@ConditionalOnClass(Advice.class)
@ConditionalOnProperty(prefix = "velo.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloLockAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LockAspect.class)
    @ConditionalOnBean({Fingerprinter.class, LockHandler.class})
    @ConditionalOnProperty(prefix = "velo.lock", name = "aspect-enabled", havingValue = "true", matchIfMissing = true)
    public LockAspect lockAspect(VeloProperties properties, Fingerprinter fingerprinter, LockHandler lockHandler) {
        return new LockAspect(properties.getLockPrefix(), fingerprinter, lockHandler);
    }
}
