package io.github.luminion.velo.lock.config;

import io.github.luminion.velo.ConcurrencyBackend;
import io.github.luminion.velo.condition.ConditionalOnVeloRedisTemplate;
import io.github.luminion.velo.condition.ConditionalOnConcurrencyBackend;
import io.github.luminion.velo.redis.VeloRedisTemplateResolver;
import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.lock.support.RedisLockHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 锁 Redis Bean 配置实现，由各 Spring Boot 版本适配模块的自动配置入口导入。
 *
 * @author luminion
 * @since 1.3.1
 */
@AutoConfiguration(after = VeloLockRedissonAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
@ConditionalOnConcurrencyBackend(prefix = "velo.lock", value = ConcurrencyBackend.REDIS,
        autoClassNames = {"org.aspectj.weaver.Advice", "org.springframework.data.redis.core.StringRedisTemplate"})
@ConditionalOnMissingBean(LockHandler.class)
@ConditionalOnProperty(prefix = "velo.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloLockRedisConfiguration {

    @Bean
    @ConditionalOnConcurrencyBackend(prefix = "velo.lock", value = ConcurrencyBackend.REDIS,
            autoBeanTypeNames = "org.springframework.data.redis.core.StringRedisTemplate")
    @ConditionalOnVeloRedisTemplate(type = "org.springframework.data.redis.core.StringRedisTemplate",
            fallbackBeanName = "stringRedisTemplate")
    @ConditionalOnMissingBean(LockHandler.class)
    public LockHandler lockHandler(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            ListableBeanFactory beanFactory,
            @Value("${velo.lock.redis-ttl-seconds:60}") long ttlSeconds) {
        StringRedisTemplate stringRedisTemplate = VeloRedisTemplateResolver.resolve(redisTemplateProvider, beanFactory,
                "org.springframework.data.redis.core.StringRedisTemplate", "stringRedisTemplate");
        return new RedisLockHandler(stringRedisTemplate, ttlSeconds);
    }
}
