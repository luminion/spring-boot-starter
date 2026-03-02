package io.github.luminion.starter.idempotent.config;

import io.github.luminion.starter.idempotent.IdempotentHandler;
import io.github.luminion.starter.idempotent.support.JdkIdempotentHandler;
import org.aspectj.weaver.Advice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 幂等自动配置 (JDK 实现 - 兜底)
 *
 * @author luminion
 * @since 1.0.0
 */
@AutoConfiguration(after = LuminionIdempotentCaffeineAutoConfiguration.class)
@ConditionalOnClass(Advice.class)
public class LuminionIdempotentJdkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdempotentHandler.class)
    public IdempotentHandler idempotentHandler() {
        return new JdkIdempotentHandler();
    }

}
