package io.github.luminion.velo.log.trace;

import io.github.luminion.velo.VeloProperties.TraceProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskDecorator;

/**
 * 独立链路上下文自动配置；不依赖 AOP 或调用日志自动配置。
 */
@AutoConfiguration
@EnableConfigurationProperties
public class VeloTraceAutoConfiguration {
    /**
     * 先读取旧配置作为默认值，再由新配置逐项覆盖。
     */
    @Bean
    @ConditionalOnMissingBean(TraceProperties.class)
    @ConfigurationProperties("velo.trace")
    public TraceProperties traceProperties(Environment environment) {
        return Binder.get(environment)
                .bind("velo.log.trace", Bindable.of(TraceProperties.class))
                .orElseGet(TraceProperties::new);
    }

    @Bean
    @ConditionalOnMissingBean(TraceContextResolver.class)
    @Conditional(TraceEnabledCondition.class)
    public TraceContextResolver traceContextResolver() {
        return new W3cTraceContextResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    @Conditional(TraceEnabledCondition.class)
    public TraceScopeManager traceScopeManager(
            TraceProperties properties, TraceContextResolver resolver) {
        return new TraceScopeManager(properties, resolver);
    }

    @Bean
    @ConditionalOnMissingBean(TaskDecorator.class)
    @Conditional(TraceEnabledCondition.class)
    public TaskDecorator mdcTaskDecorator(TraceProperties properties, TraceContextResolver resolver) {
        return new MdcTaskDecorator(properties.getMdcKey(), resolver);
    }
}
