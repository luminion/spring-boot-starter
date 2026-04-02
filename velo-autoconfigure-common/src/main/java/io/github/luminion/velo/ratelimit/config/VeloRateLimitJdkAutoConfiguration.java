package io.github.luminion.velo.ratelimit.config;

import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.ratelimit.support.JdkRateLimitHandler;
import org.aspectj.weaver.Advice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 限流自动配置 (JDK 实现)
 */
@AutoConfiguration(after = VeloRateLimitCaffeineAutoConfiguration.class)
@ConditionalOnClass(Advice.class)
@ConditionalOnProperty(prefix = "velo.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloRateLimitJdkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RateLimitHandler.class)
    @ConditionalOnProperty(prefix = "velo.rate-limit.backends", name = "jdk-enabled", havingValue = "true", matchIfMissing = true)
    public RateLimitHandler rateLimitHandler() {
        return new JdkRateLimitHandler();
    }
}
