package io.github.luminion.starter.config;

import io.github.luminion.starter.Prop;
import io.github.luminion.starter.core.fingerprint.MethodFingerprinter;
import io.github.luminion.starter.ratelimit.RateLimiter;
import io.github.luminion.starter.ratelimit.aspect.RateLimitAspect;
import io.github.luminion.starter.ratelimit.support.CaffeineRateLimiter;
import io.github.luminion.starter.ratelimit.support.GuavaRateLimiter;
import io.github.luminion.starter.ratelimit.support.JdkRateLimiter;
import io.github.luminion.starter.ratelimit.support.RedisRateLimiter;
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
 */
@AutoConfiguration
@ConditionalOnClass(Advice.class)
public class RateLimitConfig {

    @Bean
    @ConditionalOnMissingBean(RateLimitAspect.class)
    @ConditionalOnBean({ MethodFingerprinter.class, RateLimiter.class })
    public RateLimitAspect rateLimitAspect(Prop prop, MethodFingerprinter methodFingerprinter,
            RateLimiter rateLimiter) {
        return new RateLimitAspect(prop.getRateLimitPrefix(), methodFingerprinter, rateLimiter);
    }

    /**
     * 优先使用 Redis (分布式限流)
     */
    @Bean
    @Order(100)
    @ConditionalOnMissingBean(RateLimiter.class)
    @ConditionalOnBean(name = "redisTemplate")
    public RateLimiter redisRateLimiter(RedisTemplate<Object, Object> redisTemplate) {
        return new RedisRateLimiter(redisTemplate);
    }

    /**
     * 备选1: Caffeine
     */
    @Bean
    @Order(200)
    @ConditionalOnMissingBean(RateLimiter.class)
    @ConditionalOnClass(name = "com.github.benmanes.caffeine.cache.Cache")
    public RateLimiter caffeineRateLimiter() {
        return new CaffeineRateLimiter();
    }

    /**
     * 备选2: Guava
     */
    @Bean
    @Order(300)
    @ConditionalOnMissingBean(RateLimiter.class)
    @ConditionalOnClass(name = "com.google.common.cache.Cache")
    public RateLimiter guavaRateLimiter() {
        return new GuavaRateLimiter();
    }

    /**
     * 终极兜底: JDK Fixed Window
     */
    @Bean
    @Order(400)
    @ConditionalOnMissingBean(RateLimiter.class)
    public RateLimiter jdkRateLimiter() {
        return new JdkRateLimiter();
    }
}
