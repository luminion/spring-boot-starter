package io.github.luminion.autoconfigure.aop;

import io.github.luminion.autoconfigure.ConfigKey;
import io.github.luminion.autoconfigure.aop.aspect.RateLimitAspect;
import io.github.luminion.autoconfigure.aop.support.ratelimit.ConcurrentHashMapRateLimitHandler;
import io.github.luminion.autoconfigure.aop.core.RateLimitHandler;
import io.github.luminion.autoconfigure.aop.core.MethodFingerprinter;
import io.github.luminion.autoconfigure.aop.support.signature.SpELMethodFingerprinter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.Advice;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * AOP（面向切面编程）自动配置
 *
 * @author luminion
 * @see org.springframework.boot.autoconfigure.aop.AopAutoConfiguration
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({Advice.class})
@ConditionalOnProperty(value = ConfigKey.AOP_ENABLE, havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({AopProperties.class})
public class AopAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RateLimitAspect.class)
    @ConditionalOnBean({MethodFingerprinter.class, RateLimitHandler.class})
    public RateLimitAspect methodLimitAspect(BeanFactory beanFactory) {
        RateLimitAspect rateLimitAspect = new RateLimitAspect(beanFactory);
        log.debug("RateLimitAspect Configured");
        return rateLimitAspect;
    }

    @Bean
    @ConditionalOnMissingBean(MethodFingerprinter.class)
    public MethodFingerprinter spelSignatureProvider(AopProperties aopProperties) {
        log.debug("SpELMethodFingerprinter Configured");
        return new SpELMethodFingerprinter(aopProperties.getMethodLimitPrefix());
    }

    @Bean
    @ConditionalOnMissingBean(RateLimitHandler.class)
    public RateLimitHandler redisRateLimiter() {
        log.debug("ConcurrentHashMapRateLimitHandler Configured");
        return new ConcurrentHashMapRateLimitHandler();
    }

}