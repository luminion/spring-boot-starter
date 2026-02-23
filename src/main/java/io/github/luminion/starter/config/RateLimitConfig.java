package io.github.luminion.starter.config;

import io.github.luminion.starter.Prop;
import io.github.luminion.starter.core.fingerprint.MethodFingerprinter;
import io.github.luminion.starter.ratelimit.RateLimitHandler;
import io.github.luminion.starter.ratelimit.aspect.RateLimitAspect;
import io.github.luminion.starter.ratelimit.support.CaffeineRateLimitHandler;
import io.github.luminion.starter.ratelimit.support.GuavaRateLimitHandler;
import io.github.luminion.starter.ratelimit.support.JdkRateLimitHandler;
import io.github.luminion.starter.ratelimit.support.RedisRateLimitHandler;
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
    @ConditionalOnBean({ MethodFingerprinter.class, RateLimitHandler.class })
    public RateLimitAspect rateLimitAspect(Prop prop, MethodFingerprinter methodFingerprinter,
            RateLimitHandler rateLimitHandler) {
        return new RateLimitAspect(prop.getRateLimitPrefix(), methodFingerprinter, rateLimitHandler);
    }

    /**
     * 优先使用 Redis (分布式限流)
     */
    @Bean
    @Order(100)
    @ConditionalOnMissingBean(RateLimitHandler.class)
    @ConditionalOnBean(name = "redisTemplate")
    public RateLimitHandler redisRateLimiter(RedisTemplate<Object, Object> redisTemplate) {
        return new RedisRateLimitHandler(redisTemplate);
    }

    /**
     * 备选1: Caffeine
     */
    @Bean
    @Order(200)
    @ConditionalOnMissingBean(RateLimitHandler.class)
    @ConditionalOnClass(name = "com.github.benmanes.caffeine.cache.Cache")
    public RateLimitHandler caffeineRateLimiter() {
        return new CaffeineRateLimitHandler();
    }

    /**
     * 备选2: Guava
     */
    @Bean
    @Order(300)
    @ConditionalOnMissingBean(RateLimitHandler.class)
    @ConditionalOnClass(name = "com.google.common.cache.Cache")
    public RateLimitHandler guavaRateLimiter() {
        return new GuavaRateLimitHandler();
    }

    /**
     * 终极兜底: JDK Fixed Window
     */
    @Bean
    @Order(400)
    @ConditionalOnMissingBean(RateLimitHandler.class)
    public RateLimitHandler jdkRateLimiter() {
        return new JdkRateLimitHandler();
    }
}
