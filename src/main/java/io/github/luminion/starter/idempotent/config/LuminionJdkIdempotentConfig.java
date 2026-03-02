package io.github.luminion.starter.idempotent.config;

import io.github.luminion.starter.core.Prop;
import io.github.luminion.starter.core.spi.Fingerprinter;
import io.github.luminion.starter.idempotent.IdempotentHandler;
import io.github.luminion.starter.idempotent.aspect.IdempotentAspect;
import io.github.luminion.starter.idempotent.support.*;
import org.aspectj.weaver.Advice;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 幂等自动配置 (最佳实践重构)
 * 优先级: Redisson > Redis > Caffeine > Guava > JDK
 *
 * @author luminion
 */
@AutoConfiguration(after = RedisAutoConfiguration.class)
@ConditionalOnClass(Advice.class)
public class LuminionJdkIdempotentConfig {

    @Bean
    @ConditionalOnMissingBean(IdempotentAspect.class)
    @ConditionalOnBean({ Fingerprinter.class, IdempotentHandler.class })
    public IdempotentAspect idempotentAspect(Prop prop, Fingerprinter fingerprinter,
            IdempotentHandler idempotentHandler) {
        return new IdempotentAspect(prop.getIdempotentPrefix(), fingerprinter, idempotentHandler);
    }

    /**
     * 1. Redisson 实现 (高性能分布式首选)
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(IdempotentHandler.class)
    static class RedissonIdempotentConfiguration {
        @Bean
        public IdempotentHandler idempotentHandler(RedissonClient redissonClient) {
            return new RedissonIdempotentHandler(redissonClient);
        }
    }

    /**
     * 2. Redis 实现
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnBean(RedisTemplate.class)
    @ConditionalOnMissingBean(IdempotentHandler.class)
    static class RedisIdempotentConfiguration {
        @Bean
        public IdempotentHandler idempotentHandler(RedisTemplate<Object, Object> redisTemplate) {
            return new RedisIdempotentHandler(redisTemplate);
        }
    }

    /**
     * 3. Caffeine 实现 (本地缓存首选)
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.github.benmanes.caffeine.cache.Cache")
    @ConditionalOnMissingBean(IdempotentHandler.class)
    static class CaffeineIdempotentConfiguration {
        @Bean
        public IdempotentHandler idempotentHandler() {
            return new CaffeineIdempotentHandler();
        }
    }

    /**
     * 4. Guava 实现
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.google.common.cache.Cache")
    @ConditionalOnMissingBean(IdempotentHandler.class)
    static class GuavaIdempotentConfiguration {
        @Bean
        public IdempotentHandler idempotentHandler() {
            return new GuavaIdempotentHandler();
        }
    }

    /**
     * 5. JDK 实现 (底线兜底)
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(IdempotentHandler.class)
    static class JdkIdempotentConfiguration {
        @Bean
        public IdempotentHandler idempotentHandler() {
            return new JdkIdempotentHandler();
        }
    }
}
