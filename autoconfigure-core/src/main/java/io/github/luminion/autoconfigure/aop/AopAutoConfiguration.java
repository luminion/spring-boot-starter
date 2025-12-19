package io.github.luminion.autoconfigure.aop;

import io.github.luminion.autoconfigure.aop.aspect.RateLimitAspect;
import io.github.luminion.autoconfigure.aop.core.MethodFingerprinter;
import io.github.luminion.autoconfigure.aop.core.RateLimiter;
import io.github.luminion.autoconfigure.aop.support.ratelimit.JdkRateLimiter;
import io.github.luminion.autoconfigure.aop.support.signature.SpELMethodFingerprinter;
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
 * AOP（面向切面编程）自动配置
 *
 * @author luminion
 * @see org.springframework.boot.autoconfigure.aop.AopAutoConfiguration
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(Advice.class)
@ConditionalOnProperty(value = "luminion.aop.enabled", havingValue = "true", matchIfMissing = true)
public class AopAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RateLimitAspect.class)
    @ConditionalOnBean({MethodFingerprinter.class, RateLimiter.class})
    public RateLimitAspect methodLimitAspect(BeanFactory beanFactory) {
        RateLimitAspect rateLimitAspect = new RateLimitAspect(beanFactory);
        log.debug("RateLimitAspect Configured");
        return rateLimitAspect;
    }

    @Bean
    @ConditionalOnMissingBean
    public MethodFingerprinter spelSignatureProvider() {
        log.debug("SpELMethodFingerprinter Configured");
        return new SpELMethodFingerprinter("sp_e_l_method_fingerprinter");
    }

    
    
    @Bean
    @Order(100)
    @ConditionalOnMissingBean
    public RateLimiter redisRateLimiter() {
        log.debug("JdkRateLimiter Configured");
        return new JdkRateLimiter();
    }

}