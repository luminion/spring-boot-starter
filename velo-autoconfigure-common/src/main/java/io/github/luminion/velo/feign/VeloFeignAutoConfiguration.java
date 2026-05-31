package io.github.luminion.velo.feign;

import io.github.luminion.velo.VeloProperties;
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
    @ConditionalOnProperty(prefix = "velo.feign", name = "request-logging-enabled", havingValue = "true",
            matchIfMissing = true)
    public FeignLogAspect feignLogAspect(VeloProperties properties,
            ObjectProvider<RuntimeJsonSerializer> runtimeJsonSerializerProvider) {
        RuntimeJsonSerializer runtimeJsonSerializer = runtimeJsonSerializerProvider.getIfAvailable(
                () -> new HttpMessageConverterRuntimeJsonSerializer(Collections.emptyList()));
        return new FeignLogAspect(properties, runtimeJsonSerializer);
    }
}
