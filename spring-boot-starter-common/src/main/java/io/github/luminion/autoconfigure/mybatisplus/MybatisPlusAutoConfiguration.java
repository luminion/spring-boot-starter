package io.github.luminion.autoconfigure.mybatisplus;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * mybatis plus配置
 *
 * @author luminion
 * @see com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({BaseMapper.class, MybatisPlusInterceptor.class})
@ConditionalOnProperty(value = "turbo.mybatis-plus.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({MybatisPlusProperties.class})
public class MybatisPlusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(List<InnerInterceptor> interceptors) {
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        mybatisPlusInterceptor.setInterceptors(interceptors);
        log.debug("MybatisPlusInterceptor Configured with {} InnerInterceptors.", interceptors != null ? interceptors.size() : 0);
        return mybatisPlusInterceptor;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor")
    @ConditionalOnProperty(value = "turbo.mybatis-plus.pagination", havingValue = "true", matchIfMissing = true)
    static class PaginationInnerInterceptorConfiguration {
        @Bean
        @ConditionalOnMissingBean(type = "com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor")
        @SneakyThrows
        public InnerInterceptor paginationInnerInterceptor() {
            Class<?> clazz = Class.forName("com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor");
            InnerInterceptor interceptor = (InnerInterceptor) clazz.getConstructor().newInstance();
            log.debug("PaginationInnerInterceptor created");
            return interceptor;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor")
    @ConditionalOnProperty(value = "turbo.mybatis-plus.optimistic-locker", havingValue = "true", matchIfMissing = true)
    static class OptimisticLockerInnerInterceptorConfiguration {
        @Bean
        @ConditionalOnMissingBean(type = "com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor")
        @SneakyThrows
        public InnerInterceptor optimisticLockerInnerInterceptor() {
            Class<?> clazz = Class.forName("com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor");
            InnerInterceptor interceptor = (InnerInterceptor) clazz.getConstructor().newInstance();
            log.debug("OptimisticLockerInnerInterceptor created");
            return interceptor;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor")
    @ConditionalOnProperty(value = "turbo.mybatis-plus.block-attack", havingValue = "true", matchIfMissing = true)
    static class BlockAttackInnerInterceptorConfiguration {
        @Bean
        @ConditionalOnMissingBean(type = "com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor")
        @SneakyThrows
        public InnerInterceptor blockAttackInnerInterceptor() {
            Class<?> clazz = Class.forName("com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor");
            InnerInterceptor interceptor = (InnerInterceptor) clazz.getConstructor().newInstance();
            log.debug("BlockAttackInnerInterceptor created");
            return interceptor;
        }
    }
}
