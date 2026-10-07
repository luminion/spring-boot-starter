package io.github.luminion.velo.feign;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogEngine;
import io.github.luminion.velo.log.VeloLogAutoConfiguration;
import io.github.luminion.velo.log.condition.ConditionalOnInvocationAdapter;
import io.github.luminion.velo.log.trace.TraceScopeManager;
import io.github.luminion.velo.log.trace.VeloTraceAutoConfiguration;
import org.aspectj.weaver.Advice;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Feign 调试日志自动配置。
 */
@AutoConfiguration(after = {VeloLogAutoConfiguration.class, VeloTraceAutoConfiguration.class})
@ConditionalOnClass(value = Advice.class, name = "org.springframework.cloud.openfeign.FeignClient")
@ConditionalOnProperty(
        prefix = "velo.feign",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class VeloFeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnInvocationAdapter(source = "feign")
    public FeignLogAspect feignLogAspect(VeloProperties properties,
                                         ObjectProvider<InvocationLogEngine> engine,
                                         ObjectProvider<TraceScopeManager> trace) {
        FeignLogAspect aspect = new FeignLogAspect(engine.getIfAvailable(), trace.getIfAvailable());
        aspect.setOrder(properties.getAspectOrder().getFeignLog());
        return aspect;
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
