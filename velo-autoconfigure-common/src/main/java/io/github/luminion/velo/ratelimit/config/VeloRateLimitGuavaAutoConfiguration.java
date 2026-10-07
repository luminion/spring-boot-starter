package io.github.luminion.velo.ratelimit.config;

import io.github.luminion.velo.ConcurrencyBackend;
import io.github.luminion.velo.condition.ConditionalOnConcurrencyBackend;
import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.ratelimit.support.GuavaRateLimitHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Guava 原生限流自动配置。
 */
@AutoConfiguration(afterName = "io.github.luminion.velo.ratelimit.config.VeloRateLimitRedisAutoConfiguration", after = {
        VeloRateLimitRedissonAutoConfiguration.class,
        VeloRateLimitRedisConfiguration.class,
})
@ConditionalOnClass(name = "com.google.common.util.concurrent.RateLimiter")
@ConditionalOnConcurrencyBackend(prefix = "velo.rate-limit", backend = ConcurrencyBackend.GUAVA,
        autoClassNames = {"org.aspectj.weaver.Advice", "com.google.common.util.concurrent.RateLimiter"})
@ConditionalOnMissingBean(RateLimitHandler.class)
@ConditionalOnProperty(prefix = "velo.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloRateLimitGuavaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RateLimitHandler.class)
    public RateLimitHandler rateLimitHandler() {
        return new GuavaRateLimitHandler();
    }
}
