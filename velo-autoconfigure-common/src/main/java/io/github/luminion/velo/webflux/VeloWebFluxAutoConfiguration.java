package io.github.luminion.velo.webflux;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.support.Slf4JInvocationLogWriter;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.filter.reactive.ServerWebExchangeContextFilter;

import java.util.Collections;

/**
 * WebFlux 自动配置。
 *
 * <p>只在响应式 Web 应用中生效，复用现有 {@code velo.web}、{@code velo.log} 和
 * {@code velo.aspect-order} 配置。Servlet 应用仍由 {@code VeloWebAutoConfiguration} 负责。</p>
 *
 * @author luminion
 * @since 1.3.1
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.web.reactive.config.WebFluxConfigurer")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(prefix = "velo.web", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloWebFluxAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.web.reactive.config.WebFluxConfigurer")
    static class WebFluxConfiguration {

        @Bean
        @ConditionalOnMissingBean
        ServerWebExchangeContextFilter serverWebExchangeContextFilter() {
            return new ServerWebExchangeContextFilter();
        }

        @Bean
        @ConditionalOnMissingBean
        VeloWebFluxConfigurer veloWebFluxConfigurer(ObjectProvider<io.github.luminion.velo.xss.converter.XssStringConverter> provider,
                VeloProperties properties) {
            return new VeloWebFluxConfigurer(provider, properties);
        }

        @Bean
        @ConditionalOnMissingBean(RuntimeJsonSerializer.class)
        @ConditionalOnClass(ServerCodecConfigurer.class)
        RuntimeJsonSerializer runtimeJsonSerializer(ObjectProvider<ServerCodecConfigurer> codecConfigurerProvider) {
            return new DeferredWebFluxRuntimeJsonSerializer(() -> {
                ServerCodecConfigurer codecConfigurer = codecConfigurerProvider.getIfAvailable();
                return codecConfigurer == null ? Collections.emptyList() : codecConfigurer.getWriters();
            });
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "velo.log", name = {"enabled", "controller.enabled"},
                havingValue = "true", matchIfMissing = true)
        WebFluxControllerLogAspect webFluxControllerLogAspect(VeloProperties properties,
                ObjectProvider<RuntimeJsonSerializer> runtimeJsonSerializerProvider,
                ObjectProvider<InvocationLogWriter> invocationLogWriterProvider) {
            RuntimeJsonSerializer runtimeJsonSerializer = runtimeJsonSerializerProvider.getIfAvailable(
                    () -> new WebFluxRuntimeJsonSerializer(Collections.emptyList()));
            InvocationLogWriter invocationLogWriter = invocationLogWriterProvider.getIfAvailable(
                    () -> new Slf4JInvocationLogWriter(properties));
            WebFluxControllerLogAspect aspect = new WebFluxControllerLogAspect(properties, runtimeJsonSerializer,
                    invocationLogWriter);
            aspect.setOrder(properties.getAspectOrder().getControllerLog());
            return aspect;
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "velo.log", name = {"enabled", "trace.enabled"}, havingValue = "true",
                matchIfMissing = true)
        TraceIdWebFluxFilter traceIdWebFluxFilter(VeloProperties properties) {
            return new TraceIdWebFluxFilter(properties);
        }
    }
}
