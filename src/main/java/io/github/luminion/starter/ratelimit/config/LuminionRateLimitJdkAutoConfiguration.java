package io.github.luminion.starter.ratelimit.config;

import io.github.luminion.starter.idempotent.config.LuminionIdempotentCaffeineAutoConfiguration;
import io.github.luminion.starter.ratelimit.RateLimitHandler;
import io.github.luminion.starter.ratelimit.support.JdkRateLimitHandler;
import org.aspectj.weaver.Advice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 限流自动配置 (JDK 实现)
 *
 * @author luminion
 * @since 1.0.1
 */
@AutoConfiguration(after = LuminionIdempotentCaffeineAutoConfiguration.class)
@ConditionalOnClass(Advice.class)
public class LuminionRateLimitJdkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RateLimitHandler.class)
    public RateLimitHandler rateLimitHandler() {
        return new JdkRateLimitHandler();
    }

}
