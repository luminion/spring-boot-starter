package io.github.luminion.starter.autoconfig;

import io.github.luminion.starter.Prop;
import io.github.luminion.starter.core.spi.MethodFingerprinter;
import io.github.luminion.starter.ratelimit.aspect.RateLimitAspect;
import io.github.luminion.starter.ratelimit.spi.RateLimiter;
import io.github.luminion.starter.ratelimit.support.CaffeineRateLimiter;
import io.github.luminion.starter.ratelimit.support.GuavaRateLimiter;
import io.github.luminion.starter.ratelimit.support.JdkRateLimiter;
import io.github.luminion.starter.ratelimit.support.RedisRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.Advice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 限流自动配置
 *
 * @author luminion
 * @see org.springframework.boot.autoconfigure.aop.AopAutoConfiguration
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(Advice.class)
public class RateLimitConfiguration {

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
    @ConditionalOnBean({MethodFingerprinter.class, RateLimiter.class})
    public RateLimitAspect rateLimitAspect(MethodFingerprinter methodFingerprinter, RateLimiter rateLimiter) {
        return new RateLimitAspect(methodFingerprinter, rateLimiter);
    }
    
    /**
     * 配置Redis限流器（优先级最高，适用于分布式环境）
     * <p>
     * 需要RedisTemplate Bean存在才会创建
     *
     * @param redisTemplate Redis模板（Spring会自动注入类型匹配的Bean）
     * @return Redis限流器
     */
    @Bean
    @Order(100)
    @ConditionalOnMissingBean(RateLimiter.class)
    @ConditionalOnBean(name = "redisTemplate")
    public RateLimiter redisRateLimiter(RedisTemplate<Object, Object> redisTemplate, Prop prop) {
        return new RedisRateLimiter(prop.getRedisLimitPrefix(), redisTemplate);
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
