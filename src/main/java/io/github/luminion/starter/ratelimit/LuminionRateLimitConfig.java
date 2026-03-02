package io.github.luminion.starter.ratelimit;

import io.github.luminion.starter.core.Prop;
import io.github.luminion.starter.core.spi.Fingerprinter;
import io.github.luminion.starter.ratelimit.aspect.RateLimitAspect;
import org.aspectj.weaver.Advice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 限流自动配置
 * 优先级: Redisson > Redis > Caffeine > Guava > JDK
 *
 * @author luminion
 */
@AutoConfiguration
@ConditionalOnClass(Advice.class)
public class LuminionRateLimitConfig {

    @Bean
    @ConditionalOnMissingBean(RateLimitAspect.class)
    @ConditionalOnBean({ Fingerprinter.class, RateLimitHandler.class })
    public RateLimitAspect rateLimitAspect(Prop prop, Fingerprinter fingerprinter,
            RateLimitHandler rateLimitHandler) {
        return new RateLimitAspect(prop.getRateLimitPrefix(), fingerprinter, rateLimitHandler);
    }

}
