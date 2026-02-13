package io.github.luminion.starter.autoconfig;

import io.github.luminion.starter.idempotent.aspect.IdempotentAspect;
import io.github.luminion.starter.idempotent.IdempotentHandler;
import io.github.luminion.starter.core.fingerprint.MethodFingerprinter;
import io.github.luminion.starter.idempotent.support.LocalIdempotentHandler;
import io.github.luminion.starter.idempotent.support.RedisIdempotentHandler;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.Advice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 幂等自动配置
 *
 * @author luminion
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(Advice.class)
public class IdempotentConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdempotentAspect.class)
    @ConditionalOnBean({ MethodFingerprinter.class, IdempotentHandler.class })
    public IdempotentAspect idempotentAspect(MethodFingerprinter methodFingerprinter,
            IdempotentHandler idempotentHandler) {
        return new IdempotentAspect(methodFingerprinter, idempotentHandler);
    }

    @Bean
    @Order(100)
    @ConditionalOnMissingBean(IdempotentHandler.class)
    @ConditionalOnBean(name = "redisTemplate")
    public IdempotentHandler redisIdempotentHandler(RedisTemplate<Object, Object> redisTemplate) {
        return new RedisIdempotentHandler(redisTemplate);
    }

    @Bean
    @Order(200)
    @ConditionalOnMissingBean(IdempotentHandler.class)
    @ConditionalOnClass(name = "com.github.benmanes.caffeine.cache.Cache")
    public IdempotentHandler localIdempotentHandler() {
        return new LocalIdempotentHandler();
    }
}
