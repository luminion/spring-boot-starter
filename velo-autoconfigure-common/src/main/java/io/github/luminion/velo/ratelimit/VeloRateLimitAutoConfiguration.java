package io.github.luminion.velo.ratelimit;

import io.github.luminion.velo.core.VeloProperties;
import io.github.luminion.velo.core.spi.Fingerprinter;
import io.github.luminion.velo.ratelimit.aspect.RateLimitAspect;
import org.aspectj.weaver.Advice;
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

    @Bean
    @ConditionalOnMissingBean(RateLimitAspect.class)
    @ConditionalOnBean({Fingerprinter.class, RateLimitHandler.class})
    @ConditionalOnProperty(prefix = "velo.rate-limit", name = "aspect-enabled", havingValue = "true", matchIfMissing = true)
    public RateLimitAspect rateLimitAspect(VeloProperties properties, Fingerprinter fingerprinter,
            RateLimitHandler rateLimitHandler) {
        return new RateLimitAspect(properties.getRateLimitPrefix(), fingerprinter, rateLimitHandler);
    }
}
