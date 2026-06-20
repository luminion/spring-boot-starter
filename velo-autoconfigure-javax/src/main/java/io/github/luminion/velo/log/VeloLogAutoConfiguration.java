package io.github.luminion.velo.log;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.aspect.InvokeLogAspect;
import io.github.luminion.velo.log.aspect.SlowLogAspect;
import io.github.luminion.velo.log.support.Slf4JInvocationLogWriter;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import org.aspectj.weaver.Advice;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 日志自动配置。
 */
@AutoConfiguration
@ConditionalOnClass(Advice.class)
@ConditionalOnProperty(prefix = "velo.log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "velo.log.invocation", name = "enabled", havingValue = "true",
            matchIfMissing = true)
    public InvocationLogWriter invocationLogWriter(VeloProperties properties) {
        return new Slf4JInvocationLogWriter(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "velo.log.invocation", name = {"enabled", "method.enabled"},
            havingValue = "true", matchIfMissing = true)
    public InvokeLogAspect invokeLogAspect(VeloProperties properties,
            ObjectProvider<RuntimeJsonSerializer> runtimeJsonSerializerProvider,
            InvocationLogWriter invocationLogWriter) {
        InvokeLogAspect aspect = new InvokeLogAspect(properties, runtimeJsonSerializerProvider, invocationLogWriter);
        aspect.setOrder(properties.getAspectOrder().getInvokeLog());
        return aspect;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "velo.log.invocation", name = {"enabled", "method.enabled"},
            havingValue = "true", matchIfMissing = true)
    public SlowLogAspect slowLogAspect(VeloProperties properties,
            ObjectProvider<RuntimeJsonSerializer> runtimeJsonSerializerProvider,
            InvocationLogWriter invocationLogWriter) {
        SlowLogAspect aspect = new SlowLogAspect(properties, runtimeJsonSerializerProvider, invocationLogWriter);
        aspect.setOrder(properties.getAspectOrder().getSlowLog());
        return aspect;
    }
}
