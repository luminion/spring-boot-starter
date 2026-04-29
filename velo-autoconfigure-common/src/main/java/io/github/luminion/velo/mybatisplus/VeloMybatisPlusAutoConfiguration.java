package io.github.luminion.velo.mybatisplus;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * mybatis plus配置
 *
 * @author luminion
 * @see com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration
 */
@AutoConfiguration
@ConditionalOnClass({BaseMapper.class, MybatisPlusInterceptor.class})
@ConditionalOnProperty(prefix = "velo.mybatis-plus", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloMybatisPlusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(List<InnerInterceptor> interceptors) {
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        mybatisPlusInterceptor.setInterceptors(interceptors);
        return mybatisPlusInterceptor;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor")
    static class PaginationInnerInterceptorConfiguration {
        @Bean
        @ConditionalOnMissingBean(type = "com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor")
        @ConditionalOnProperty(prefix = "velo.mybatis-plus", name = "pagination-enabled", havingValue = "true", matchIfMissing = true)
        @SneakyThrows
        public InnerInterceptor paginationInnerInterceptor() {
            Class<?> clazz = Class.forName("com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor");
            InnerInterceptor interceptor = (InnerInterceptor) clazz.getConstructor().newInstance();
            return interceptor;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor")
    static class OptimisticLockerInnerInterceptorConfiguration {
        @Bean
        @ConditionalOnMissingBean(type = "com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor")
        @ConditionalOnProperty(prefix = "velo.mybatis-plus", name = "optimistic-locker-enabled", havingValue = "true", matchIfMissing = true)
        @SneakyThrows
        public InnerInterceptor optimisticLockerInnerInterceptor() {
            Class<?> clazz = Class.forName("com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor");
            InnerInterceptor interceptor = (InnerInterceptor) clazz.getConstructor().newInstance();
            return interceptor;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor")
    static class BlockAttackInnerInterceptorConfiguration {
        @Bean
        @ConditionalOnMissingBean(type = "com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor")
        @ConditionalOnProperty(prefix = "velo.mybatis-plus", name = "block-attack-enabled", havingValue = "true", matchIfMissing = true)
        @SneakyThrows
        public InnerInterceptor blockAttackInnerInterceptor() {
            Class<?> clazz = Class.forName("com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor");
            InnerInterceptor interceptor = (InnerInterceptor) clazz.getConstructor().newInstance();
            return interceptor;
        }
    }
}
