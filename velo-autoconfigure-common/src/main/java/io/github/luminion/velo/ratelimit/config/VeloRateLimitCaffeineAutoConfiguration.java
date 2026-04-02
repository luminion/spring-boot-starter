package io.github.luminion.velo.ratelimit.config;

import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.ratelimit.support.CaffeineRateLimitHandler;
import org.aspectj.weaver.Advice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
@ConditionalOnClass({ Advice.class, com.github.benmanes.caffeine.cache.Cache.class })
@ConditionalOnProperty(prefix = "velo.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloRateLimitCaffeineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RateLimitHandler.class)
    @ConditionalOnProperty(prefix = "velo.rate-limit.backends", name = "caffeine-enabled", havingValue = "true", matchIfMissing = true)
    public RateLimitHandler rateLimitHandler() {
        return new CaffeineRateLimitHandler();
    }

}
