package io.github.luminion.autoconfigure.ratelimit;

import io.github.luminion.autoconfigure.core.spi.KeyResolver;
import io.github.luminion.autoconfigure.ratelimit.aspect.RateLimitAspect;
import io.github.luminion.autoconfigure.ratelimit.spi.RateLimiter;
import io.github.luminion.autoconfigure.ratelimit.support.JdkRateLimiter;
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

/**
 * 限流自动配置
 * @author luminion
 * @since 1.0.0
 * @see org.springframework.boot.autoconfigure.aop.AopAutoConfiguration
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(Advice.class)
@ConditionalOnProperty(value = "luminion.aop.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean(RateLimitAspect.class)
    @ConditionalOnBean({KeyResolver.class, RateLimiter.class})
    public RateLimitAspect methodLimitAspect(BeanFactory beanFactory) {
        RateLimitAspect rateLimitAspect = new RateLimitAspect(beanFactory);
        log.debug("RateLimitAspect Configured");
        return rateLimitAspect;
    }
    
    // todo 自动注入不同的限流实现

    @Bean
    @Order(100)
    @ConditionalOnMissingBean
    public RateLimiter redisRateLimiter() {
        log.debug("JdkRateLimiter Configured");
        return new JdkRateLimiter();
    }
    
}
