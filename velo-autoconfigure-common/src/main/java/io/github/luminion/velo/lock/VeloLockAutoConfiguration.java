package io.github.luminion.velo.lock;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.VeloMessageResolver;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.lock.aspect.LockAspect;
import org.aspectj.weaver.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        io.github.luminion.velo.lock.config.VeloLockRedisAutoConfiguration.class,
        io.github.luminion.velo.lock.config.VeloLockCaffeineAutoConfiguration.class,
        io.github.luminion.velo.lock.config.VeloLockJdkAutoConfiguration.class
})
@ConditionalOnClass(Advice.class)
@ConditionalOnProperty(prefix = "velo.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloLockAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(VeloLockAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(LockAspect.class)
    @ConditionalOnBean({Fingerprinter.class, LockHandler.class})
    public LockAspect lockAspect(VeloProperties properties, Fingerprinter fingerprinter, LockHandler lockHandler,
            ObjectProvider<VeloMessageResolver> messageResolver) {
        log.info("[Velo Starter] Lock enabled, backend handler: {}", lockHandler.getClass().getSimpleName());
        LockAspect aspect = new LockAspect(properties.getLock().getPrefix(), fingerprinter, lockHandler,
                messageResolver.getIfAvailable());
        aspect.setOrder(properties.getAspectOrder().getLock());
        return aspect;
    }
}
