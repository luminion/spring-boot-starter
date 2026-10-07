package io.github.luminion.velo.feign;

import feign.RequestInterceptor;
import io.github.luminion.velo.VeloProperties.TraceProperties;
import io.github.luminion.velo.log.trace.TraceContextResolver;
import io.github.luminion.velo.log.trace.TraceEnabledCondition;
import io.github.luminion.velo.log.trace.VeloTraceAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Feign 链路传播自动配置；不依赖调用日志和 AOP。
 */
@AutoConfiguration(after = VeloTraceAutoConfiguration.class)
@ConditionalOnClass(RequestInterceptor.class)
@Conditional(VeloFeignTraceAutoConfiguration.PropagationEnabledCondition.class)
public class VeloFeignTraceAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public FeignTraceRequestInterceptor feignTraceRequestInterceptor(TraceProperties properties, TraceContextResolver resolver) {
        return new FeignTraceRequestInterceptor(properties, resolver);
    }

    static class PropagationEnabledCondition extends SpringBootCondition {
        @Override
        public ConditionOutcome getMatchOutcome(
                ConditionContext context, AnnotatedTypeMetadata metadata) {
            Boolean property = TraceEnabledCondition.property(context.getEnvironment(), "feign-propagation-enabled", Boolean.class, true);
            boolean enabled = TraceEnabledCondition.isEnabled(context.getEnvironment()) && property;
            return new ConditionOutcome(enabled, "trace Feign propagation enabled");
        }
    }
}
