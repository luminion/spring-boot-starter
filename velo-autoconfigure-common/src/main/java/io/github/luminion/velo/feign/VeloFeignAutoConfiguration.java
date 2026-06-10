package io.github.luminion.velo.feign;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.support.Slf4JInvocationLogWriter;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import io.github.luminion.velo.spi.provider.HttpMessageConverterRuntimeJsonSerializer;
import org.aspectj.weaver.Advice;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.Collections;

/**
 * Feign 调试日志自动配置。
 */
@AutoConfiguration
@ConditionalOnClass(value = Advice.class, name = "org.springframework.cloud.openfeign.FeignClient")
@ConditionalOnProperty(prefix = "velo.feign", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloFeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "velo.log", name = {"enabled", "invocation.enabled", "invocation.feign.enabled"},
            havingValue = "true",
            matchIfMissing = true)
    public FeignLogAspect feignLogAspect(VeloProperties properties,
            ObjectProvider<InvocationLogWriter> invocationLogWriterProvider,
            ObjectProvider<RuntimeJsonSerializer> runtimeJsonSerializerProvider) {
        InvocationLogWriter invocationLogWriter = invocationLogWriterProvider.getIfAvailable(
                () -> new Slf4JInvocationLogWriter(properties));
        RuntimeJsonSerializer runtimeJsonSerializer = runtimeJsonSerializerProvider.getIfAvailable(
                () -> new HttpMessageConverterRuntimeJsonSerializer(Collections.emptyList()));
        return new FeignLogAspect(properties, runtimeJsonSerializer, invocationLogWriter);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "feign.RequestInterceptor")
    @ConditionalOnProperty(prefix = "velo.log", name = {"enabled", "trace.enabled", "trace.feign-propagation-enabled"},
            havingValue = "true",
            matchIfMissing = true)
    public FeignTraceRequestInterceptor feignTraceRequestInterceptor(VeloProperties properties) {
        return new FeignTraceRequestInterceptor(properties);
    }
}
