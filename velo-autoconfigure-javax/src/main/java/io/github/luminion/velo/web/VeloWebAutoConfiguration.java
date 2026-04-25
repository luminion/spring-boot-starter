package io.github.luminion.velo.web;

import io.github.luminion.velo.core.VeloProperties;
import io.github.luminion.velo.core.spi.RuntimeJsonSerializer;
import io.github.luminion.velo.core.spi.provider.HttpMessageConverterRuntimeJsonSerializer;
import io.github.luminion.velo.xss.converter.XssStringConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * Web MVC 自动配置。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "velo.web", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "velo.web", name = "mvc-configurer-enabled", havingValue = "true", matchIfMissing = true)
    public VeloWebMvcConfigurer veloWebMvcConfigurer(ObjectProvider<XssStringConverter> xssStringConverterProvider,
            VeloProperties properties) {
        return new VeloWebMvcConfigurer(xssStringConverterProvider, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(RequestMappingHandlerAdapter.class)
    public RuntimeJsonSerializer runtimeJsonSerializer(ObjectProvider<RequestMappingHandlerAdapter> handlerAdapterProvider) {
        RequestMappingHandlerAdapter handlerAdapter = handlerAdapterProvider.getIfAvailable();
        return new HttpMessageConverterRuntimeJsonSerializer(
                handlerAdapter == null ? java.util.Collections.emptyList() : handlerAdapter.getMessageConverters());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(ControllerLogAspect.class)
    @ConditionalOnProperty(prefix = "velo.web.request-logging", name = "enabled", havingValue = "true")
    public ControllerLogAspect controllerLogAspect(VeloProperties properties, RuntimeJsonSerializer runtimeJsonSerializer) {
        return new ControllerLogAspect(properties, runtimeJsonSerializer);
    }
}
