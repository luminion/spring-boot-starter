package io.github.luminion.starter.idempotent;

import io.github.luminion.starter.core.Prop;
import io.github.luminion.starter.core.spi.Fingerprinter;
import io.github.luminion.starter.idempotent.aspect.IdempotentAspect;
import io.github.luminion.starter.idempotent.support.RedissonIdempotentHandler;
import org.aspectj.weaver.Advice;
import org.redisson.api.RedissonClient;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 幂等自动配置
 *
 * @author luminion
 * @since 1.0.0
 */
@AutoConfiguration
public class LuminionIdempotentConfig {

    @Bean
    @ConditionalOnMissingBean(IdempotentAspect.class)
    @ConditionalOnBean({ Fingerprinter.class, IdempotentHandler.class })
    public IdempotentAspect idempotentAspect(Prop prop, Fingerprinter fingerprinter,
            IdempotentHandler idempotentHandler) {
        return new IdempotentAspect(prop.getIdempotentPrefix(), fingerprinter, idempotentHandler);
    }

}
