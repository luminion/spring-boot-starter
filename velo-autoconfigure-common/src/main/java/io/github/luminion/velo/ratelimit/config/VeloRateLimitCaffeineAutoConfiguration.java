package io.github.luminion.velo.ratelimit.config;

import io.github.luminion.velo.core.ConcurrencyBackend;
import io.github.luminion.velo.core.condition.ConditionalOnConcurrencyBackend;
import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.ratelimit.support.CaffeineRateLimitHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 限流自动配置 (Caffeine 实现)
 *
 * @author luminion
 * @since 1.0.0
 */
@AutoConfiguration(after = VeloRateLimitRedisAutoConfiguration.class)
@ConditionalOnConcurrencyBackend(prefix = "velo.rate-limit", value = ConcurrencyBackend.CAFFEINE,
        autoClassNames = {"org.aspectj.weaver.Advice", "com.github.benmanes.caffeine.cache.Cache"})
@ConditionalOnMissingBean(RateLimitHandler.class)
@ConditionalOnProperty(prefix = "velo.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloRateLimitCaffeineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RateLimitHandler.class)
    public RateLimitHandler rateLimitHandler() {
        return new CaffeineRateLimitHandler();
    }

}
