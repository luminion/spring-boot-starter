package io.github.luminion.velo.feign;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.support.Slf4JInvocationLogWriter;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import io.github.luminion.velo.spi.provider.HttpMessageConverterRuntimeJsonSerializer;
import org.aspectj.weaver.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(VeloFeignAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "velo.log", name = {"enabled", "invocation.enabled", "invocation.feign.enabled"},
            havingValue = "true",
            matchIfMissing = true)
    public FeignLogAspect feignLogAspect(VeloProperties properties,
            ObjectProvider<InvocationLogWriter> invocationLogWriterProvider,
            ObjectProvider<RuntimeJsonSerializer> runtimeJsonSerializerProvider) {
        InvocationLogWriter invocationLogWriter = invocationLogWriterProvider.getIfAvailable(
                () -> {
                    log.debug("No InvocationLogWriter bean found, using Slf4JInvocationLogWriter for FeignLogAspect");
                    return new Slf4JInvocationLogWriter(properties);
                });
        RuntimeJsonSerializer runtimeJsonSerializer = runtimeJsonSerializerProvider.getIfAvailable(
                () -> {
                    log.debug("No RuntimeJsonSerializer bean found, using empty HTTP message converter list for FeignLogAspect");
                    return new HttpMessageConverterRuntimeJsonSerializer(Collections.emptyList());
                });
        FeignLogAspect aspect = new FeignLogAspect(properties, runtimeJsonSerializer, invocationLogWriter);
        aspect.setOrder(properties.getAspectOrder().getFeignLog());
        return aspect;
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
