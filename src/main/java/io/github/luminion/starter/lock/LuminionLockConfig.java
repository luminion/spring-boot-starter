package io.github.luminion.starter.lock;

import io.github.luminion.starter.core.Prop;
import io.github.luminion.starter.core.spi.Fingerprinter;
import io.github.luminion.starter.lock.aspect.LockAspect;
import org.aspectj.weaver.Advice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 锁自动配置
 * 优先级: Redisson > Redis > JDK
 *
 * @author luminion
 * @since 1.0.1
 */
@AutoConfiguration
@ConditionalOnClass(Advice.class)
public class LuminionLockConfig {

    @Bean
    @ConditionalOnMissingBean(LockAspect.class)
    @ConditionalOnBean({ Fingerprinter.class, LockHandler.class })
    public LockAspect lockAspect(Prop prop, Fingerprinter fingerprinter, LockHandler lockHandler) {
        return new LockAspect(prop.getLockPrefix(), fingerprinter, lockHandler);
    }

}
