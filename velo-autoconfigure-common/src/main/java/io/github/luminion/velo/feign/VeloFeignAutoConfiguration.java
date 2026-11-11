package io.github.luminion.velo.feign;

import io.github.luminion.velo.core.DeferredInterceptorAdvisor;
import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.log.config.VeloLogAutoConfiguration;
import io.github.luminion.velo.log.config.condition.ConditionalOnInvocationAdapter;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.trace.TraceScopeManager;
import io.github.luminion.velo.trace.VeloTraceAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.env.Environment;

/**
 * Feign 调试日志自动配置。
 */
@AutoConfiguration(after = {VeloLogAutoConfiguration.class, VeloTraceAutoConfiguration.class})
@ConditionalOnClass(name = "org.springframework.cloud.openfeign.FeignClient")
@ConditionalOnProperty(
        prefix = "velo.feign",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class VeloFeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnInvocationAdapter(source = "feign")
    public FeignLogAspect feignLogAspect(ObjectProvider<InvocationLogEngine> engine,
                                         ObjectProvider<TraceScopeManager> trace, Environment environment) {
        return new FeignLogAspect(engine.getIfAvailable(), trace.getIfAvailable(), environment);
    }

    /**
     * 只携带切点和顺序的基础设施 Advisor；拦截器在容器单例初始化完成后解析，避免提前创建业务依赖。
     */
    @Bean(name = "feignLogAdvisor")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(name = "feignLogAdvisor")
    @ConditionalOnBean(FeignLogAspect.class)
    public static DeferredInterceptorAdvisor feignLogAdvisor(Environment environment,
                                                             ObjectProvider<FeignLogAspect> feignLogAspect) {
        int order = Binder.get(environment).bind("velo.aspect-order.feign-log", Integer.class)
                .orElse(VeloAdvisorOrder.LOG_FEIGN);
        return new DeferredInterceptorAdvisor(FeignLogAspect.pointcut(), order, feignLogAspect::getObject);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "feign.Capability")
    @ConditionalOnProperty(
            prefix = "velo.log",
            name = {"enabled", "sources.feign.enabled"},
            havingValue = "true",
            matchIfMissing = true)
    public FeignInvocationCapability feignInvocationCapability() {
        return new FeignInvocationCapability();
    }
}
