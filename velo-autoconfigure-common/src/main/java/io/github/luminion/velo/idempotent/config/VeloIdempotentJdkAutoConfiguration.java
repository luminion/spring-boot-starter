package io.github.luminion.velo.idempotent.config;

import io.github.luminion.velo.ConcurrencyBackend;
import io.github.luminion.velo.condition.ConditionalOnConcurrencyBackend;
import io.github.luminion.velo.idempotent.IdempotentHandler;
import io.github.luminion.velo.idempotent.support.JdkIdempotentHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 幂等自动配置 (JDK 实现 - 兜底)
 *
 * @author luminion
 * @since 1.0.0
 */
@AutoConfiguration(after = VeloIdempotentCaffeineAutoConfiguration.class)
@ConditionalOnConcurrencyBackend(prefix = "velo.idempotent", value = ConcurrencyBackend.JDK,
        autoClassNames = "org.aspectj.weaver.Advice")
@ConditionalOnMissingBean(IdempotentHandler.class)
@ConditionalOnProperty(prefix = "velo.idempotent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloIdempotentJdkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdempotentHandler.class)
    public IdempotentHandler idempotentHandler() {
        return new JdkIdempotentHandler();
    }

}
