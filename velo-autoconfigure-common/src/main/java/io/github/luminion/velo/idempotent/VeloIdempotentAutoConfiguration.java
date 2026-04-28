package io.github.luminion.velo.idempotent;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.idempotent.aspect.IdempotentAspect;
import org.aspectj.weaver.Advice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 幂等自动配置实现。
 */
@AutoConfiguration(after = {
        io.github.luminion.velo.idempotent.config.VeloIdempotentRedissonAutoConfiguration.class,
        io.github.luminion.velo.idempotent.config.VeloIdempotentRedisAutoConfiguration.class,
        io.github.luminion.velo.idempotent.config.VeloIdempotentCaffeineAutoConfiguration.class,
        io.github.luminion.velo.idempotent.config.VeloIdempotentJdkAutoConfiguration.class
})
@ConditionalOnClass(Advice.class)
@ConditionalOnProperty(prefix = "velo.idempotent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloIdempotentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdempotentAspect.class)
    @ConditionalOnBean({Fingerprinter.class, IdempotentHandler.class})
    public IdempotentAspect idempotentAspect(VeloProperties properties, Fingerprinter fingerprinter,
            IdempotentHandler idempotentHandler) {
        return new IdempotentAspect(properties.getIdempotent().getKeyPrefix(), fingerprinter, idempotentHandler);
    }
}
