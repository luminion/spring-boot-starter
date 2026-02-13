package io.github.luminion.starter.config;

import io.github.luminion.starter.Prop;
import io.github.luminion.starter.ratelimit.aspect.RateLimitAspect;
import io.github.luminion.starter.core.fingerprint.MethodFingerprinter;
import io.github.luminion.starter.ratelimit.RateLimiter;
import io.github.luminion.starter.ratelimit.support.LocalRateLimiter;
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
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(Advice.class)
public class RateLimitConfig {

    @Bean
    @ConditionalOnMissingBean(RateLimitAspect.class)
    @ConditionalOnBean({MethodFingerprinter.class, RateLimiter.class})
    public RateLimitAspect rateLimitAspect(Prop prop, MethodFingerprinter methodFingerprinter, RateLimiter rateLimiter) {
        return new RateLimitAspect(prop.getRateLimitPrefix(), methodFingerprinter, rateLimiter);
    }

    @Bean
    @Order(100)
    @ConditionalOnMissingBean(RateLimiter.class)
    @ConditionalOnBean(name = "redisTemplate")
    public RateLimiter redisRateLimiter(RedisTemplate<Object, Object> redisTemplate) {
        log.debug("RateLimiter: RedisRateLimiter configured.");
        return new RedisRateLimiter(redisTemplate);
    }

    @Bean
    @Order(200)
    @ConditionalOnMissingBean(RateLimiter.class)
    @ConditionalOnClass(name = "com.github.benmanes.caffeine.cache.Cache")
    public RateLimiter localRateLimiter() {
        log.debug("RateLimiter: LocalRateLimiter (Caffeine) configured.");
        return new LocalRateLimiter();
    }
}
