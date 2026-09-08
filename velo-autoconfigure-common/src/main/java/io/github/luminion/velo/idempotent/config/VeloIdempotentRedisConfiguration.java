package io.github.luminion.velo.idempotent.config;

import io.github.luminion.velo.ConcurrencyBackend;
import io.github.luminion.velo.condition.ConditionalOnVeloRedisTemplate;
import io.github.luminion.velo.condition.ConditionalOnConcurrencyBackend;
import io.github.luminion.velo.redis.VeloRedisTemplateResolver;
import io.github.luminion.velo.idempotent.IdempotentHandler;
import io.github.luminion.velo.idempotent.support.RedisIdempotentHandler;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 幂等 Redis Bean 配置实现，由各 Spring Boot 版本适配模块的自动配置入口导入。
 *
 * @author luminion
 * @since 1.3.1
 */
@AutoConfiguration(after = VeloIdempotentRedissonAutoConfiguration.class)
@ConditionalOnConcurrencyBackend(prefix = "velo.idempotent", value = ConcurrencyBackend.REDIS,
        autoClassNames = {"org.aspectj.weaver.Advice", "org.springframework.data.redis.core.RedisTemplate"})
@ConditionalOnMissingBean(IdempotentHandler.class)
@ConditionalOnProperty(prefix = "velo.idempotent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloIdempotentRedisConfiguration {

    @Bean
    @ConditionalOnConcurrencyBackend(prefix = "velo.idempotent", value = ConcurrencyBackend.REDIS,
            autoBeanTypeNames = "org.springframework.data.redis.core.RedisTemplate")
    @ConditionalOnVeloRedisTemplate(type = "org.springframework.data.redis.core.RedisTemplate",
            fallbackBeanName = "redisTemplate")
    @ConditionalOnMissingBean(IdempotentHandler.class)
    public IdempotentHandler idempotentHandler(ObjectProvider<RedisTemplate<Object, Object>> redisTemplateProvider,
            ListableBeanFactory beanFactory) {
        RedisTemplate<Object, Object> redisTemplate = VeloRedisTemplateResolver.resolve(redisTemplateProvider, beanFactory,
                "org.springframework.data.redis.core.RedisTemplate", "redisTemplate");
        return new RedisIdempotentHandler(redisTemplate);
    }
}
