package io.github.luminion.velo.idempotent.config;

import io.github.luminion.velo.core.ConcurrencyBackend;
import io.github.luminion.velo.core.condition.ConditionalOnConcurrencyBackend;
import io.github.luminion.velo.idempotent.IdempotentHandler;
import io.github.luminion.velo.idempotent.support.CaffeineIdempotentHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 幂等自动配置 (Caffeine 实现)
 *
 * @author luminion
 * @since 1.0.0
 */
@AutoConfiguration(after = VeloIdempotentRedisAutoConfiguration.class)
@ConditionalOnConcurrencyBackend(prefix = "velo.idempotent", value = ConcurrencyBackend.CAFFEINE,
        autoClassNames = {"org.aspectj.weaver.Advice", "com.github.benmanes.caffeine.cache.Cache"})
@ConditionalOnMissingBean(IdempotentHandler.class)
@ConditionalOnProperty(prefix = "velo.idempotent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloIdempotentCaffeineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdempotentHandler.class)
    public IdempotentHandler idempotentHandler() {
        return new CaffeineIdempotentHandler();
    }

}
