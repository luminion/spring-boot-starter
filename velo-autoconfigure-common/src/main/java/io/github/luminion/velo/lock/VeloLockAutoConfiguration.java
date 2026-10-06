package io.github.luminion.velo.lock;

import io.github.luminion.velo.ConcurrencyBackend;
import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.VeloMessageResolver;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.lock.aspect.LockAspect;
import org.aspectj.weaver.Advice;
import org.springframework.beans.factory.ObjectProvider;
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
        io.github.luminion.velo.lock.config.VeloLockJdkAutoConfiguration.class
}, afterName = "io.github.luminion.velo.lock.config.VeloLockRedisAutoConfiguration")
@ConditionalOnClass(Advice.class)
@ConditionalOnProperty(prefix = "velo.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloLockAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LockHandler.class)
    public LockHandler unavailableLockHandler(VeloProperties properties) {
        ConcurrencyBackend backend = properties.getLock().getBackend();
        if (backend != ConcurrencyBackend.AUTO) {
            throw new IllegalStateException("No LockHandler is available for velo.lock.backend=" + backend
                    + ". Supported backends: REDISSON, REDIS, JDK, or a custom LockHandler.");
        }
        return new LockHandler() {
            @Override
            public boolean tryLock(String key) {
                throw new IllegalStateException("No LockHandler is available. Configure a supported backend "
                        + "or provide a custom LockHandler.");
            }

            @Override
            public void unlock(String key) {
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(LockAspect.class)
    @ConditionalOnBean({Fingerprinter.class, LockHandler.class})
    public LockAspect lockAspect(VeloProperties properties, Fingerprinter fingerprinter, LockHandler lockHandler,
            ObjectProvider<VeloMessageResolver> messageResolver) {
        LockAspect aspect = new LockAspect(properties.getLock().getPrefix(), fingerprinter, lockHandler,
                messageResolver.getIfAvailable());
        aspect.setOrder(properties.getAspectOrder().getLock());
        return aspect;
    }
}
