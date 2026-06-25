package io.github.luminion.velo.ratelimit;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.VeloMessageResolver;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.ratelimit.aspect.RateLimitAspect;
import org.aspectj.weaver.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        io.github.luminion.velo.ratelimit.config.VeloRateLimitRedisAutoConfiguration.class,
        io.github.luminion.velo.ratelimit.config.VeloRateLimitCaffeineAutoConfiguration.class,
        io.github.luminion.velo.ratelimit.config.VeloRateLimitJdkAutoConfiguration.class
})
@ConditionalOnClass(Advice.class)
@ConditionalOnProperty(prefix = "velo.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloRateLimitAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(VeloRateLimitAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(RateLimitAspect.class)
    @ConditionalOnBean({Fingerprinter.class, RateLimitHandler.class})
    public RateLimitAspect rateLimitAspect(VeloProperties properties, Fingerprinter fingerprinter,
            RateLimitHandler rateLimitHandler, ObjectProvider<VeloMessageResolver> messageResolver) {
        log.info("[Velo Starter] RateLimit enabled, backend handler: {}", rateLimitHandler.getClass().getSimpleName());
        RateLimitAspect aspect = new RateLimitAspect(properties.getRateLimit().getPrefix(), fingerprinter,
                rateLimitHandler, messageResolver.getIfAvailable());
        aspect.setOrder(properties.getAspectOrder().getRateLimit());
        return aspect;
    }
}
