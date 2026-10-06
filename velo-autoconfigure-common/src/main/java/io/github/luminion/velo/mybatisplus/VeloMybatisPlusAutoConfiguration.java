package io.github.luminion.velo.mybatisplus;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * mybatis plus配置
 *
 * @author luminion
// * @see com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration
 */
@AutoConfiguration
@ConditionalOnClass(name = {
        "com.baomidou.mybatisplus.core.mapper.BaseMapper",
        "com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor"
})
@ConditionalOnProperty(prefix = "velo.mybatis-plus", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloMybatisPlusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(type = "com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor")
    public MybatisPlusInterceptor mybatisPlusInterceptor(List<InnerInterceptor> interceptors) {
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        mybatisPlusInterceptor.setInterceptors(interceptors);
        return mybatisPlusInterceptor;
    }

    /**
     * 条件阶段无法从接口返回类型判断用户实例；创建默认 Bean 前再按实际类型退让。
     * 只解析用户 Bean，避免默认拦截器之间互相触发创建造成循环依赖。
     */
    @SneakyThrows
    private static InnerInterceptor createDefaultInterceptor(String className, ListableBeanFactory beanFactory) {
        Class<?> type = Class.forName(className);
        for (String name : beanFactory.getBeanNamesForType(InnerInterceptor.class)) {
            if (name.equals("paginationInnerInterceptor") || name.equals("optimisticLockerInnerInterceptor")
                    || name.equals("blockAttackInnerInterceptor")) {
                continue;
            }
            InnerInterceptor candidate = beanFactory.getBean(name, InnerInterceptor.class);
            if (type.isInstance(candidate)) {
                return null;
            }
        }
        return (InnerInterceptor) type.getConstructor().newInstance();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor")
    static class PaginationInnerInterceptorConfiguration {
        @Bean
        @Order(Ordered.LOWEST_PRECEDENCE - 100)
        @ConditionalOnMissingBean(type = "com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor",
                name = "paginationInnerInterceptor")
        @ConditionalOnProperty(prefix = "velo.mybatis-plus", name = "pagination-enabled", havingValue = "true", matchIfMissing = true)
        public InnerInterceptor paginationInnerInterceptor(ListableBeanFactory beanFactory) {
            return createDefaultInterceptor("com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor", beanFactory);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor")
    static class OptimisticLockerInnerInterceptorConfiguration {
        @Bean
        @Order(Ordered.LOWEST_PRECEDENCE - 300)
        @ConditionalOnMissingBean(type = "com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor",
                name = "optimisticLockerInnerInterceptor")
        @ConditionalOnProperty(prefix = "velo.mybatis-plus", name = "optimistic-locker-enabled", havingValue = "true", matchIfMissing = true)
        public InnerInterceptor optimisticLockerInnerInterceptor(ListableBeanFactory beanFactory) {
            return createDefaultInterceptor("com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor", beanFactory);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor")
    static class BlockAttackInnerInterceptorConfiguration {
        @Bean
        @Order(Ordered.LOWEST_PRECEDENCE - 200)
        @ConditionalOnMissingBean(type = "com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor",
                name = "blockAttackInnerInterceptor")
        @ConditionalOnProperty(prefix = "velo.mybatis-plus", name = "block-attack-enabled", havingValue = "true", matchIfMissing = true)
        public InnerInterceptor blockAttackInnerInterceptor(ListableBeanFactory beanFactory) {
            return createDefaultInterceptor("com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor", beanFactory);
        }
    }
}
