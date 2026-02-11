package io.github.luminion.autoconfigure.repeat;

import io.github.luminion.autoconfigure.core.spi.KeyResolver;
import io.github.luminion.autoconfigure.repeat.aspect.RepeatSubmitAspect;
import io.github.luminion.autoconfigure.repeat.spi.RepeatSubmitHandler;
import io.github.luminion.autoconfigure.repeat.support.GuavaRepeatSubmitHandler;
import io.github.luminion.autoconfigure.repeat.support.MemoryRepeatSubmitHandler;
import io.github.luminion.autoconfigure.repeat.support.RedisRepeatSubmitHandler;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.Advice;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 防重复提交自动配置
 * <p>
 * 自动配置防重复提交相关的Bean，包括：
 * 1. RepeatSubmitAspect：防重复提交切面，处理@RepeatSubmit注解
 * 2. RepeatSubmitHandler实现：根据类路径自动选择最优的处理器实现
 *    优先级：RedisRepeatSubmitHandler > MemoryRepeatSubmitHandler > GuavaRepeatSubmitHandler
 *
 * @author luminion
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(Advice.class)
@ConditionalOnProperty(value = "luminion.repeat-submit.enabled", havingValue = "true", matchIfMissing = true)
public class RepeatSubmitAutoConfiguration {
    
    /**
     * 配置防重复提交切面
     * <p>
     * 需要同时存在KeyResolver和RepeatSubmitHandler Bean才会创建此切面
     *
     * @param beanFactory Bean工厂，用于获取KeyResolver和RepeatSubmitHandler实例
     * @return 防重复提交切面
     */
    @Bean
    @ConditionalOnMissingBean(RepeatSubmitAspect.class)
    @ConditionalOnBean({KeyResolver.class, RepeatSubmitHandler.class})
    public RepeatSubmitAspect repeatSubmitAspect(BeanFactory beanFactory) {
        log.debug("RepeatSubmitAspect Configured");
        return new RepeatSubmitAspect(beanFactory);
    }
    
    /**
     * 配置Redis防重复提交处理器（优先级最高，适用于分布式环境）
     * <p>
     * 需要RedisTemplate Bean存在才会创建
     *
     * @param redisTemplate Redis模板
     * @return Redis防重复提交处理器
     */
    @Bean
    @Order(100)
    @ConditionalOnMissingBean(RepeatSubmitHandler.class)
    @ConditionalOnBean(name = "redisTemplate")
    @SuppressWarnings("unchecked")
    public RepeatSubmitHandler redisRepeatSubmitHandler(org.springframework.data.redis.core.RedisTemplate<?, ?> redisTemplate) {
        log.debug("RedisRepeatSubmitHandler Configured");
        return new RedisRepeatSubmitHandler((RedisTemplate<Object, Object>) redisTemplate);
    }

    /**
     * 配置内存防重复提交处理器（优先级次高，适用于单机环境，使用Caffeine）
     * <p>
     * 需要Caffeine库存在才会创建
     * 注意：For Java 11 or above, use 3.x otherwise use 2.x.
     *
     * @return 内存防重复提交处理器
     */
    @Bean
    @Order(200)
    @ConditionalOnMissingBean(RepeatSubmitHandler.class)
    @ConditionalOnClass(name = "com.github.benmanes.caffeine.cache.Cache")
    public RepeatSubmitHandler memoryRepeatSubmitHandler() {
        log.debug("MemoryRepeatSubmitHandler Configured");
        return new MemoryRepeatSubmitHandler();
    }

    /**
     * 配置Guava防重复提交处理器（优先级第三，适用于单机环境）
     * <p>
     * 需要Guava库存在才会创建
     *
     * @return Guava防重复提交处理器
     */
    @Bean
    @Order(300)
    @ConditionalOnMissingBean(RepeatSubmitHandler.class)
    @ConditionalOnClass(name = "com.google.common.cache.Cache")
    public RepeatSubmitHandler guavaRepeatSubmitHandler() {
        log.debug("GuavaRepeatSubmitHandler Configured");
        return new GuavaRepeatSubmitHandler();
    }
    
}

