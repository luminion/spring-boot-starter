package io.github.luminion.velo.core;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.idempotent.IdempotentHandler;
import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.spi.JsonProcessorProvider;
import io.github.luminion.velo.spi.NamingSuffixStrategy;
import io.github.luminion.velo.spi.fingerprint.SpelFingerprinter;
import io.github.luminion.velo.spi.provider.DefaultJsonProcessorProvider;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 核心基础能力自动配置。
 */
@AutoConfiguration
@EnableConfigurationProperties(VeloProperties.class)
public class VeloCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Fingerprinter fingerprinter() {
        return new SpelFingerprinter();
    }

    @Bean
    @ConditionalOnMissingBean
    public NamingSuffixStrategy namingSuffixStrategy() {
        return () -> "Name";
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonProcessorProvider jsonProcessorProvider(BeanFactory beanFactory) {
        return new DefaultJsonProcessorProvider(beanFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public VeloMessageResolver veloMessageResolver() {
        return new VeloMessageResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public VeloBannerPrinter veloBannerPrinter(VeloProperties properties,
            ObjectProvider<IdempotentHandler> idempotentHandler,
            ObjectProvider<RateLimitHandler> rateLimitHandler,
            ObjectProvider<LockHandler> lockHandler) {
        return new VeloBannerPrinter(properties, idempotentHandler, rateLimitHandler, lockHandler);
    }
}
