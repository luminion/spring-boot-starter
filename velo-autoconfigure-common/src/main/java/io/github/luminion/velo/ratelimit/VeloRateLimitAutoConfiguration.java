package io.github.luminion.velo.ratelimit;

import io.github.luminion.velo.ConcurrencyBackend;
import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.VeloMessageResolver;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.ratelimit.aspect.RateLimitAspect;
import org.aspectj.weaver.Advice;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 限流自动配置实现。
 */
@AutoConfiguration(after = {
        io.github.luminion.velo.ratelimit.config.VeloRateLimitRedissonAutoConfiguration.class,
        io.github.luminion.velo.ratelimit.config.VeloRateLimitRedisConfiguration.class,
        io.github.luminion.velo.ratelimit.config.VeloRateLimitGuavaAutoConfiguration.class
})
@ConditionalOnClass(Advice.class)
@ConditionalOnProperty(prefix = "velo.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloRateLimitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RateLimitHandler.class)
    public RateLimitHandler unavailableRateLimitHandler(VeloProperties properties) {
        ConcurrencyBackend backend = properties.getRateLimit().getBackend();
        if (backend != ConcurrencyBackend.AUTO) {
            throw new IllegalStateException("No RateLimitHandler is available for velo.rate-limit.backend="
                    + backend + ". Supported backends: REDISSON, REDIS, GUAVA, or a custom RateLimitHandler.");
        }
        return (key, qps) -> {
            throw new IllegalStateException("No RateLimitHandler is available. Configure a supported backend "
                    + "or provide a custom RateLimitHandler.");
        };
    }

    @Bean
    @ConditionalOnMissingBean(RateLimitAspect.class)
    @ConditionalOnBean({Fingerprinter.class, RateLimitHandler.class})
    public RateLimitAspect rateLimitAspect(VeloProperties properties, Fingerprinter fingerprinter,
                                           RateLimitHandler rateLimitHandler, ObjectProvider<VeloMessageResolver> messageResolver) {
        RateLimitAspect aspect = new RateLimitAspect(properties.getRateLimit().getPrefix(), fingerprinter,
                rateLimitHandler, messageResolver.getIfAvailable());
        aspect.setOrder(properties.getAspectOrder().getRateLimit());
        return aspect;
    }
}
