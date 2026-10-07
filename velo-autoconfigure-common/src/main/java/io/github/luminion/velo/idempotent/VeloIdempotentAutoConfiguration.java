package io.github.luminion.velo.idempotent;

import io.github.luminion.velo.ConcurrencyBackend;
import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.VeloMessageResolver;
import io.github.luminion.velo.idempotent.config.VeloIdempotentCaffeineAutoConfiguration;
import io.github.luminion.velo.idempotent.config.VeloIdempotentRedisConfiguration;
import io.github.luminion.velo.idempotent.config.VeloIdempotentRedissonAutoConfiguration;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.idempotent.aspect.IdempotentAspect;
import org.aspectj.weaver.Advice;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 幂等自动配置实现。
 */
@AutoConfiguration(afterName = "io.github.luminion.velo.idempotent.config.VeloIdempotentRedisAutoConfiguration", after = {
        VeloIdempotentRedissonAutoConfiguration.class,
        VeloIdempotentRedisConfiguration.class,
        VeloIdempotentCaffeineAutoConfiguration.class
})
@ConditionalOnClass(Advice.class)
@ConditionalOnProperty(prefix = "velo.idempotent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloIdempotentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdempotentHandler.class)
    public IdempotentHandler unavailableIdempotentHandler(VeloProperties properties) {
        ConcurrencyBackend backend = properties.getIdempotent().getBackend();
        if (backend != ConcurrencyBackend.AUTO) {
            throw new IllegalStateException("No IdempotentHandler is available for velo.idempotent.backend="
                    + backend + ". Supported backends: REDISSON, REDIS, CAFFEINE, or a custom IdempotentHandler.");
        }
        return (key, token, timeout) -> {
            throw new IllegalStateException("No IdempotentHandler is available. Add Caffeine, configure Redis/Redisson "
                    + "or provide a custom IdempotentHandler.");
        };
    }

    @Bean
    @ConditionalOnMissingBean(IdempotentAspect.class)
    @ConditionalOnBean({Fingerprinter.class, IdempotentHandler.class})
    public IdempotentAspect idempotentAspect(VeloProperties properties, Fingerprinter fingerprinter,
                                             IdempotentHandler idempotentHandler, ObjectProvider<VeloMessageResolver> messageResolver) {
        return new IdempotentAspect(properties.getIdempotent().getPrefix(), fingerprinter,
                idempotentHandler, messageResolver.getIfAvailable(), properties.getAspectOrder().getIdempotent(),
                properties.getIdempotent().getMessage());
    }
}
