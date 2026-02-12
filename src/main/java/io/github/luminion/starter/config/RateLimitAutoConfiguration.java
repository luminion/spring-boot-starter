package io.github.luminion.starter.config;

import io.github.luminion.starter.core.spi.KeyResolver;
import io.github.luminion.starter.support.ratelimit.aspect.RateLimitAspect;
import io.github.luminion.starter.support.ratelimit.spi.RateLimiter;
import io.github.luminion.starter.support.ratelimit.support.CaffeineRateLimiter;
import io.github.luminion.starter.support.ratelimit.support.GuavaRateLimiter;
import io.github.luminion.starter.support.ratelimit.support.JdkRateLimiter;
import io.github.luminion.starter.support.ratelimit.support.RedisRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.Advice;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 限流自动配置
 * <p>
 * 自动配置限流相关的Bean，包括：
 * 1. RateLimitAspect：限流切面，处理@RateLimit注解
 * 2. RateLimiter实现：根据类路径自动选择最优的限流器实现
 *    优先级：RedisRateLimiter > CaffeineRateLimiter > GuavaRateLimiter > JdkRateLimiter
 *
 * @author luminion
 * @since 1.0.0
 * @see org.springframework.boot.autoconfigure.aop.AopAutoConfiguration
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(Advice.class)
@ConditionalOnProperty(value = "luminion.ratelimit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitAutoConfiguration {
    
    /**
     * 配置限流切面
     * <p>
     * 需要同时存在KeyResolver和RateLimiter Bean才会创建此切面
     *
     * @param beanFactory Bean工厂，用于获取KeyResolver和RateLimiter实例
     * @return 限流切面
     */
    @Bean
    @ConditionalOnMissingBean(RateLimitAspect.class)
    @ConditionalOnBean({KeyResolver.class, RateLimiter.class})
    public RateLimitAspect rateLimitAspect(BeanFactory beanFactory) {
        log.debug("RateLimitAspect Configured");
        return new RateLimitAspect(beanFactory);
    }
    
    /**
     * 配置Redis限流器（优先级最高，适用于分布式环境）
     * <p>
     * 需要RedisTemplate Bean存在才会创建
     * 注意：RedisTemplate的泛型应该是<Object, Object>以匹配RedisRateLimiter的要求
     * 这里使用@SuppressWarnings("rawtypes")来避免泛型检查问题，因为Spring Boot的RedisTemplate通常是类型擦除的
     *
     * @param redisTemplate Redis模板（Spring会自动注入类型匹配的Bean）
     * @return Redis限流器
     */
    @Bean
    @Order(100)
    @ConditionalOnMissingBean(RateLimiter.class)
    @ConditionalOnBean(name = "redisTemplate")
    @SuppressWarnings("unchecked")
    public RateLimiter redisRateLimiter(org.springframework.data.redis.core.RedisTemplate<?, ?> redisTemplate) {
        log.debug("RedisRateLimiter Configured");
        // 强制转换为RedisTemplate<Object, Object>，因为RedisRateLimiter需要这个类型
        // 在运行时，RedisTemplate的实际类型是擦除的，所以这个转换是安全的
        return new RedisRateLimiter((RedisTemplate<Object, Object>) redisTemplate);
    }

    /**
     * 配置Caffeine限流器（优先级次高，适用于单机环境，内存安全）
     * <p>
     * 需要Caffeine库存在才会创建
     * 注意：For Java 11 or above, use 3.x otherwise use 2.x.
     *
     * @return Caffeine限流器
     */
    @Bean
    @Order(200)
    @ConditionalOnMissingBean(RateLimiter.class)
    @ConditionalOnClass(name = "com.github.benmanes.caffeine.cache.Cache")
    public RateLimiter caffeineRateLimiter() {
        log.debug("CaffeineRateLimiter Configured");
        return new CaffeineRateLimiter();
    }

    /**
     * 配置Guava限流器（优先级第三，适用于单机环境）
     * <p>
     * 需要Guava库存在才会创建
     *
     * @return Guava限流器
     */
    @Bean
    @Order(300)
    @ConditionalOnMissingBean(RateLimiter.class)
    @ConditionalOnClass(name = "com.google.common.util.concurrent.RateLimiter")
    public RateLimiter guavaRateLimiter() {
        log.debug("GuavaRateLimiter Configured");
        return new GuavaRateLimiter();
    }

    /**
     * 配置JDK限流器（兜底方案，不推荐在生产环境使用）
     * <p>
     * 警告：此实现存在内存泄漏风险，当使用动态key时会无限增长
     * 仅作为其他实现都不可用时的最终兜底方案
     *
     * @return JDK限流器
     */
    @Bean
    @Order(400)
    @ConditionalOnMissingBean(RateLimiter.class)
    public RateLimiter jdkRateLimiter() {
        log.warn("JdkRateLimiter Configured as fallback. This implementation has memory leak risk and is not recommended for production use. " +
                "Please consider using RedisRateLimiter, CaffeineRateLimiter, or GuavaRateLimiter instead.");
        return new JdkRateLimiter();
    }
    
}
