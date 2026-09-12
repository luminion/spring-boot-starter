package io.github.luminion.velo.webflux;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.VeloMessageResolver;
import io.github.luminion.velo.idempotent.IdempotentHandler;
import io.github.luminion.velo.idempotent.VeloIdempotentAutoConfiguration;
import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.lock.VeloLockAutoConfiguration;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.VeloLogAutoConfiguration;
import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.ratelimit.VeloRateLimitAutoConfiguration;
import io.github.luminion.velo.spi.Fingerprinter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * WebFlux 响应式注解切面自动配置。
 *
 * <p>该配置与 {@link VeloWebFluxAutoConfiguration} 分离：关闭 {@code velo.web.enabled}
 * 只关闭 Web 层增强，不会意外关闭响应式幂等、限流、锁和方法日志能力。</p>
 *
 * @author luminion
 * @since 1.3.1
 */
@AutoConfiguration(after = {
        VeloWebFluxAutoConfiguration.class,
        VeloIdempotentAutoConfiguration.class,
        VeloLockAutoConfiguration.class,
        VeloRateLimitAutoConfiguration.class,
        VeloLogAutoConfiguration.class
})
@ConditionalOnClass(name = {
        "org.aspectj.weaver.Advice",
        "org.springframework.web.reactive.config.WebFluxConfigurer"
})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class VeloWebFluxReactiveAspectAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(WebFluxIdempotentAspect.class)
    @ConditionalOnBean({Fingerprinter.class, IdempotentHandler.class})
    @ConditionalOnProperty(prefix = "velo.idempotent", name = "enabled", havingValue = "true",
            matchIfMissing = true)
    public WebFluxIdempotentAspect webFluxIdempotentAspect(VeloProperties properties,
            Fingerprinter fingerprinter, IdempotentHandler idempotentHandler,
            ObjectProvider<VeloMessageResolver> messageResolver) {
        WebFluxIdempotentAspect aspect = new WebFluxIdempotentAspect(properties.getIdempotent().getPrefix(),
                fingerprinter, idempotentHandler, messageResolver.getIfAvailable());
        aspect.setOrder(properties.getAspectOrder().getIdempotent());
        return aspect;
    }

    @Bean
    @ConditionalOnMissingBean(WebFluxLockAspect.class)
    @ConditionalOnBean({Fingerprinter.class, LockHandler.class})
    @ConditionalOnProperty(prefix = "velo.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
    public WebFluxLockAspect webFluxLockAspect(VeloProperties properties, Fingerprinter fingerprinter,
            LockHandler lockHandler, ObjectProvider<VeloMessageResolver> messageResolver) {
        WebFluxLockAspect aspect = new WebFluxLockAspect(properties.getLock().getPrefix(), fingerprinter,
                lockHandler, messageResolver.getIfAvailable());
        aspect.setOrder(properties.getAspectOrder().getLock());
        return aspect;
    }

    @Bean
    @ConditionalOnMissingBean(WebFluxRateLimitAspect.class)
    @ConditionalOnBean({Fingerprinter.class, RateLimitHandler.class})
    @ConditionalOnProperty(prefix = "velo.rate-limit", name = "enabled", havingValue = "true",
            matchIfMissing = true)
    public WebFluxRateLimitAspect webFluxRateLimitAspect(VeloProperties properties, Fingerprinter fingerprinter,
            RateLimitHandler rateLimitHandler, ObjectProvider<VeloMessageResolver> messageResolver) {
        WebFluxRateLimitAspect aspect = new WebFluxRateLimitAspect(properties.getRateLimit().getPrefix(),
                fingerprinter, rateLimitHandler, messageResolver.getIfAvailable());
        aspect.setOrder(properties.getAspectOrder().getRateLimit());
        return aspect;
    }

    @Bean
    @ConditionalOnMissingBean(WebFluxInvokeLogAspect.class)
    @ConditionalOnBean(InvocationLogWriter.class)
    @ConditionalOnProperty(prefix = "velo.log", name = "enabled", havingValue = "true", matchIfMissing = true)
    public WebFluxInvokeLogAspect webFluxInvokeLogAspect(VeloProperties properties,
            ObjectProvider<io.github.luminion.velo.spi.RuntimeJsonSerializer> runtimeJsonSerializerProvider,
            InvocationLogWriter invocationLogWriter) {
        WebFluxInvokeLogAspect aspect = new WebFluxInvokeLogAspect(properties, runtimeJsonSerializerProvider,
                invocationLogWriter);
        aspect.setOrder(properties.getAspectOrder().getInvokeLog());
        return aspect;
    }

    @Bean
    @ConditionalOnMissingBean(WebFluxSlowLogAspect.class)
    @ConditionalOnBean(InvocationLogWriter.class)
    @ConditionalOnProperty(prefix = "velo.log", name = "enabled", havingValue = "true", matchIfMissing = true)
    public WebFluxSlowLogAspect webFluxSlowLogAspect(VeloProperties properties,
            ObjectProvider<io.github.luminion.velo.spi.RuntimeJsonSerializer> runtimeJsonSerializerProvider,
            InvocationLogWriter invocationLogWriter) {
        WebFluxSlowLogAspect aspect = new WebFluxSlowLogAspect(properties, runtimeJsonSerializerProvider,
                invocationLogWriter);
        aspect.setOrder(properties.getAspectOrder().getSlowLog());
        return aspect;
    }
}
