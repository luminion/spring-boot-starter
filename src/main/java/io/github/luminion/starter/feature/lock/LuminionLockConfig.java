package io.github.luminion.starter.feature.lock;

import io.github.luminion.starter.core.Prop;
import io.github.luminion.starter.core.spi.Fingerprinter;
import io.github.luminion.starter.feature.lock.aspect.LockAspect;
import io.github.luminion.starter.feature.lock.support.JdkLockHandler;
import io.github.luminion.starter.feature.lock.support.RedisLockHandler;
import io.github.luminion.starter.feature.lock.support.RedissonLockHandler;
import org.aspectj.weaver.Advice;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 锁自动配置
 * 优先级: Redisson > Redis > JDK
 *
 * @author luminion
 * @since 1.0.1
 */
@AutoConfiguration(after = RedisAutoConfiguration.class)
@ConditionalOnClass(Advice.class)
public class LuminionLockConfig {

    @Bean
    @ConditionalOnMissingBean(LockAspect.class)
    @ConditionalOnBean({ Fingerprinter.class, LockHandler.class })
    public LockAspect lockAspect(Prop prop, Fingerprinter fingerprinter, LockHandler lockHandler) {
        return new LockAspect(prop.getLockPrefix(), fingerprinter, lockHandler);
    }

    /**
     * 1. Redisson 实现 (分布式锁最佳实践)
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(LockHandler.class)
    static class RedissonLockConfiguration {
        @Bean
        public LockHandler lockHandler(RedissonClient redissonClient) {
            return new RedissonLockHandler(redissonClient);
        }
    }

    /**
     * 2. Redis 原生 Lua 实现
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(name = "stringRedisTemplate")
    @ConditionalOnMissingBean(LockHandler.class)
    static class RedisLockConfiguration {
        @Bean
        public LockHandler lockHandler(StringRedisTemplate stringRedisTemplate) {
            return new RedisLockHandler(stringRedisTemplate);
        }
    }

    /**
     * 3. JDK 实现 (本地兜底)
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(LockHandler.class)
    static class JdkLockConfiguration {
        @Bean
        public LockHandler lockHandler() {
            return new JdkLockHandler();
        }
    }
}
