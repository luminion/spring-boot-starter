package io.github.luminion.starter.ratelimit.config;

import io.github.luminion.starter.ratelimit.RateLimitHandler;
import io.github.luminion.starter.ratelimit.support.CaffeineRateLimitHandler;
import org.aspectj.weaver.Advice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 限流自动配置 (Caffeine 实现)
 *
 * @author luminion
 * @since 1.0.0
 */
@AutoConfiguration(after = LuminionRateLimitRedisAutoConfiguration.class)
@ConditionalOnClass({ Advice.class, com.github.benmanes.caffeine.cache.Cache.class })
public class LuminionRateLimitCaffeineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RateLimitHandler.class)
    public RateLimitHandler rateLimitHandler() {
        return new CaffeineRateLimitHandler();
    }

}
