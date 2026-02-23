package io.github.luminion.starter.config;

import io.github.luminion.starter.Prop;
import io.github.luminion.starter.core.fingerprint.MethodFingerprinter;
import io.github.luminion.starter.idempotent.IdempotentHandler;
import io.github.luminion.starter.idempotent.aspect.IdempotentAspect;
import io.github.luminion.starter.idempotent.support.CaffeineIdempotentHandler;
import io.github.luminion.starter.idempotent.support.GuavaIdempotentHandler;
import io.github.luminion.starter.idempotent.support.JdkIdempotentHandler;
import io.github.luminion.starter.idempotent.support.RedisIdempotentHandler;
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
@AutoConfiguration
@ConditionalOnClass(Advice.class)
public class IdempotentConfig {

    @Bean
    @ConditionalOnMissingBean(IdempotentAspect.class)
    @ConditionalOnBean({ MethodFingerprinter.class, IdempotentHandler.class })
    public IdempotentAspect idempotentAspect(Prop prop, MethodFingerprinter methodFingerprinter,
            IdempotentHandler idempotentHandler) {
        return new IdempotentAspect(prop.getIdempotentPrefix(), methodFingerprinter, idempotentHandler);
    }

    /**
     * 优先使用 Redis 方案
     */
    @Bean
    @Order(100)
    @ConditionalOnMissingBean(IdempotentHandler.class)
    @ConditionalOnBean(name = "redisTemplate")
    public IdempotentHandler redisIdempotentHandler(RedisTemplate<Object, Object> redisTemplate) {
        return new RedisIdempotentHandler(redisTemplate);
    }

    /**
     * 备选方案1: Caffeine
     */
    @Bean
    @Order(200)
    @ConditionalOnMissingBean(IdempotentHandler.class)
    @ConditionalOnClass(name = "com.github.benmanes.caffeine.cache.Cache")
    public IdempotentHandler caffeineIdempotentHandler() {
        return new CaffeineIdempotentHandler();
    }

    /**
     * 备选方案2: Guava
     */
    @Bean
    @Order(300)
    @ConditionalOnMissingBean(IdempotentHandler.class)
    @ConditionalOnClass(name = "com.google.common.cache.Cache")
    public IdempotentHandler guavaIdempotentHandler() {
        return new GuavaIdempotentHandler();
    }

    /**
     * 终极兜底方案: JDK ConcurrentHashMap
     */
    @Bean
    @Order(400)
    @ConditionalOnMissingBean(IdempotentHandler.class)
    public IdempotentHandler jdkIdempotentHandler() {
        return new JdkIdempotentHandler();
    }
}
