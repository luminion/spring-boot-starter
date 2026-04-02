package io.github.luminion.velo.lock.config;

import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.lock.support.JdkLockHandler;
import org.aspectj.weaver.Advice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 锁自动配置 (JDK 实现)
 *
 * @author luminion
 * @since 1.0.0
 */
@AutoConfiguration(after = VeloLockRedisAutoConfiguration.class)
@ConditionalOnClass(Advice.class)
@ConditionalOnProperty(prefix = "velo.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloLockJdkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LockHandler.class)
    @ConditionalOnProperty(prefix = "velo.lock.backends", name = "jdk-enabled", havingValue = "true", matchIfMissing = true)
    public LockHandler lockHandler() {
        return new JdkLockHandler();
    }

}
