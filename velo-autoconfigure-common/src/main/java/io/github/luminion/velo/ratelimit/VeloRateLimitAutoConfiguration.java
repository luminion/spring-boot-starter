package io.github.luminion.velo.ratelimit;

import io.github.luminion.velo.ConcurrencyBackend;
import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.VeloMessageResolver;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.ratelimit.aspect.RateLimitAspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import io.github.luminion.velo.core.DeferredInterceptorAdvisor;
import io.github.luminion.velo.core.VeloAdvisorOrder;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Role;
import org.springframework.core.env.Environment;

/**
 * 限流自动配置实现。
 */
@AutoConfiguration(afterName = "io.github.luminion.velo.ratelimit.config.VeloRateLimitRedisAutoConfiguration", after = {
        io.github.luminion.velo.ratelimit.config.VeloRateLimitRedissonAutoConfiguration.class,
        io.github.luminion.velo.ratelimit.config.VeloRateLimitRedisConfiguration.class,
        io.github.luminion.velo.ratelimit.config.VeloRateLimitGuavaAutoConfiguration.class
})
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
        return new RateLimitAspect(properties.getRateLimit().getPrefix(), fingerprinter,
                rateLimitHandler, messageResolver.getIfAvailable(),
                properties.getRateLimit().getMessage());
    }

    /**
     * 只携带切点和顺序的基础设施 Advisor；拦截器在容器单例初始化完成后解析，避免提前创建业务依赖。
     */
    @Bean(name = "rateLimitAdvisor")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(name = "rateLimitAdvisor")
    @ConditionalOnBean(RateLimitAspect.class)
    public static DeferredInterceptorAdvisor rateLimitAdvisor(Environment environment, ObjectProvider<RateLimitAspect> rateLimitAspect) {
        int order = Binder.get(environment).bind("velo.aspect-order.rate-limit", Integer.class).orElse(VeloAdvisorOrder.CONCURRENCY_RATE_LIMIT);
        return new DeferredInterceptorAdvisor(RateLimitAspect.pointcut(), order, rateLimitAspect::getObject);
    }
}
