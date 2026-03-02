package io.github.luminion.starter.lock.config;

import io.github.luminion.starter.lock.LockHandler;
import io.github.luminion.starter.lock.support.JdkLockHandler;
import org.aspectj.weaver.Advice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 锁自动配置 (JDK 实现)
 *
 * @author luminion
 * @since 1.0.1
 */
@AutoConfiguration(after = LuminionLockRedisAutoConfiguration.class)
@ConditionalOnClass(Advice.class)
public class LuminionLockJdkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LockHandler.class)
    public LockHandler lockHandler() {
        return new JdkLockHandler();
    }

}
