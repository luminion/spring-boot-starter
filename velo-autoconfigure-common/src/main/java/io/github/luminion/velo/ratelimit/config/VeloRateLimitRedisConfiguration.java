package io.github.luminion.velo.ratelimit.config;

import io.github.luminion.velo.ConcurrencyBackend;
import io.github.luminion.velo.condition.ConditionalOnConcurrencyBackend;
import io.github.luminion.velo.condition.ConditionalOnVeloRedisTemplate;
import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.ratelimit.support.RedisRateLimitHandler;
import io.github.luminion.velo.redis.VeloRedisTemplateResolver;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 限流 Bean 配置，由各 Spring Boot 版本的自动配置入口导入。
 */
@AutoConfiguration(after = VeloRateLimitRedissonAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
@ConditionalOnConcurrencyBackend(prefix = "velo.rate-limit", value = ConcurrencyBackend.REDIS,
        autoClassNames = {"org.aspectj.weaver.Advice", "org.springframework.data.redis.core.StringRedisTemplate"})
@ConditionalOnMissingBean(RateLimitHandler.class)
@ConditionalOnProperty(prefix = "velo.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloRateLimitRedisConfiguration {

    @Bean
    @ConditionalOnConcurrencyBackend(prefix = "velo.rate-limit", value = ConcurrencyBackend.REDIS,
            autoBeanTypeNames = "org.springframework.data.redis.core.StringRedisTemplate")
    @ConditionalOnVeloRedisTemplate(type = "org.springframework.data.redis.core.StringRedisTemplate",
            fallbackBeanName = "stringRedisTemplate")
    @ConditionalOnMissingBean(RateLimitHandler.class)
    public RateLimitHandler rateLimitHandler(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                             ListableBeanFactory beanFactory) {
        return new RedisRateLimitHandler(VeloRedisTemplateResolver.resolve(redisTemplateProvider, beanFactory,
                "org.springframework.data.redis.core.StringRedisTemplate", "stringRedisTemplate"));
    }
}
