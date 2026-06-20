package io.github.luminion.velo.web;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.support.Slf4JInvocationLogWriter;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import io.github.luminion.velo.spi.provider.HttpMessageConverterRuntimeJsonSerializer;
import io.github.luminion.velo.xss.converter.XssStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(VeloWebAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public VeloWebMvcConfigurer veloWebMvcConfigurer(ObjectProvider<XssStringConverter> xssStringConverterProvider,
            VeloProperties properties) {
        return new VeloWebMvcConfigurer(xssStringConverterProvider, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(RequestMappingHandlerAdapter.class)
    public RuntimeJsonSerializer runtimeJsonSerializer(ObjectProvider<RequestMappingHandlerAdapter> handlerAdapterProvider) {
        RequestMappingHandlerAdapter handlerAdapter = handlerAdapterProvider.getIfAvailable();
        if (handlerAdapter == null) {
            log.debug("No RequestMappingHandlerAdapter bean found, using empty HTTP message converter list for RuntimeJsonSerializer");
            return new HttpMessageConverterRuntimeJsonSerializer(java.util.Collections.emptyList());
        }
        return new HttpMessageConverterRuntimeJsonSerializer(
                handlerAdapter.getMessageConverters());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(ControllerLogAspect.class)
    @ConditionalOnProperty(prefix = "velo.log", name = {"enabled", "invocation.enabled", "invocation.controller.enabled"},
            havingValue = "true",
            matchIfMissing = true)
    public ControllerLogAspect controllerLogAspect(VeloProperties properties, RuntimeJsonSerializer runtimeJsonSerializer,
            ObjectProvider<InvocationLogWriter> invocationLogWriterProvider) {
        InvocationLogWriter invocationLogWriter = invocationLogWriterProvider.getIfAvailable(
                () -> {
                    log.debug("No InvocationLogWriter bean found, using Slf4JInvocationLogWriter for ControllerLogAspect");
                    return new Slf4JInvocationLogWriter(properties);
                });
        ControllerLogAspect aspect = new ControllerLogAspect(properties, runtimeJsonSerializer, invocationLogWriter);
        aspect.setOrder(properties.getAspectOrder().getControllerLog());
        return aspect;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "velo.log", name = {"enabled", "trace.enabled"}, havingValue = "true",
            matchIfMissing = true)
    public TraceIdFilter traceIdFilter(VeloProperties properties) {
        return new TraceIdFilter(properties);
    }
}
