package io.github.luminion.velo.ratelimit.config;

import io.github.luminion.velo.ConcurrencyBackend;
import io.github.luminion.velo.condition.ConditionalOnVeloRedisTemplate;
import io.github.luminion.velo.condition.ConditionalOnConcurrencyBackend;
import io.github.luminion.velo.redis.VeloRedisTemplateResolver;
import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.ratelimit.support.RedisRateLimitHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 限流 Redis Bean 配置实现，由各 Spring Boot 版本适配模块的自动配置入口导入。
 *
 * @author luminion
 * @since 1.3.1
 */
@AutoConfiguration(after = VeloRateLimitRedissonAutoConfiguration.class)
@ConditionalOnConcurrencyBackend(prefix = "velo.rate-limit", value = ConcurrencyBackend.REDIS,
        autoClassNames = {"org.aspectj.weaver.Advice", "org.springframework.data.redis.core.RedisTemplate"})
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
        StringRedisTemplate stringRedisTemplate = VeloRedisTemplateResolver.resolve(redisTemplateProvider, beanFactory,
                "org.springframework.data.redis.core.StringRedisTemplate", "stringRedisTemplate");
        return new RedisRateLimitHandler(stringRedisTemplate);
    }
}
