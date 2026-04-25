package io.github.luminion.velo.ratelimit.config;

import io.github.luminion.velo.core.ConcurrencyBackend;
import io.github.luminion.velo.core.condition.ConditionalOnConcurrencyBackend;
import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.ratelimit.support.JdkRateLimitHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 限流自动配置 (JDK 实现)
 */
@AutoConfiguration(after = VeloRateLimitCaffeineAutoConfiguration.class)
@ConditionalOnConcurrencyBackend(prefix = "velo.rate-limit", value = ConcurrencyBackend.JDK,
        autoClassNames = "org.aspectj.weaver.Advice")
@ConditionalOnMissingBean(RateLimitHandler.class)
@ConditionalOnProperty(prefix = "velo.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloRateLimitJdkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RateLimitHandler.class)
    public RateLimitHandler rateLimitHandler() {
        return new JdkRateLimitHandler();
    }
}
