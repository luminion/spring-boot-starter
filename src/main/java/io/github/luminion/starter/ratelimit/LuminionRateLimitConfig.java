package io.github.luminion.starter.ratelimit;

import io.github.luminion.starter.core.Prop;
import io.github.luminion.starter.core.spi.Fingerprinter;
import io.github.luminion.starter.ratelimit.aspect.RateLimitAspect;
import io.github.luminion.starter.ratelimit.support.*;
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
 * 限流自动配置
 * 优先级: Redisson > Redis > Caffeine > Guava > JDK
 *
 * @author luminion
 */
@AutoConfiguration(after = RedisAutoConfiguration.class)
@ConditionalOnClass(Advice.class)
public class LuminionRateLimitConfig {

    @Bean
    @ConditionalOnMissingBean(RateLimitAspect.class)
    @ConditionalOnBean({ Fingerprinter.class, RateLimitHandler.class })
    public RateLimitAspect rateLimitAspect(Prop prop, Fingerprinter fingerprinter,
            RateLimitHandler rateLimitHandler) {
        return new RateLimitAspect(prop.getRateLimitPrefix(), fingerprinter, rateLimitHandler);
    }

    /**
     * 1. Redisson 实现 (令牌桶算法)
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RedissonClient.class)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(RateLimitHandler.class)
    static class RedissonRateLimitConfiguration {
        @Bean
        public RateLimitHandler rateLimitHandler(RedissonClient redissonClient) {
            return new RedissonRateLimitHandler(redissonClient);
        }
    }

    /**
     * 2. Redis 实现 (Lua 脚本)
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnBean(RedisTemplate.class)
    @ConditionalOnMissingBean(RateLimitHandler.class)
    static class RedisRateLimitConfiguration {
        @Bean
        public RateLimitHandler rateLimitHandler(RedisTemplate<Object, Object> redisTemplate) {
            return new RedisRateLimitHandler(redisTemplate);
        }
    }

    /**
     * 3. Caffeine 实现
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.github.benmanes.caffeine.cache.Cache")
    @ConditionalOnMissingBean(RateLimitHandler.class)
    static class CaffeineRateLimitConfiguration {
        @Bean
        public RateLimitHandler rateLimitHandler() {
            return new CaffeineRateLimitHandler();
        }
    }

    /**
     * 4. Guava 实现
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.google.common.cache.Cache")
    @ConditionalOnMissingBean(RateLimitHandler.class)
    static class GuavaRateLimitConfiguration {
        @Bean
        public RateLimitHandler rateLimitHandler() {
            return new GuavaRateLimitHandler();
        }
    }

    /**
     * 5. JDK 实现
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(RateLimitHandler.class)
    static class JdkRateLimitConfiguration {
        @Bean
        public RateLimitHandler rateLimitHandler() {
            return new JdkRateLimitHandler();
        }
    }
}
