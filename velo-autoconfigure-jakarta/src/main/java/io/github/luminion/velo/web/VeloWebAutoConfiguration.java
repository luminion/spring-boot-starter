package io.github.luminion.velo.web;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogEngine;
import io.github.luminion.velo.log.VeloLogAutoConfiguration;
import io.github.luminion.velo.log.trace.TraceContextResolver;
import io.github.luminion.velo.xss.converter.XssStringConverter;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/** Web MVC 自动配置。 */
@AutoConfiguration(after = VeloLogAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
    prefix = "velo.web",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class VeloWebAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public VeloWebMvcConfigurer veloWebMvcConfigurer(
      ObjectProvider<XssStringConverter> xssStringConverterProvider, VeloProperties properties) {
    return new VeloWebMvcConfigurer(xssStringConverterProvider, properties);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnClass(ControllerLogAspect.class)
  @ConditionalOnProperty(
      prefix = "velo.log",
      name = {"enabled", "sources.controller.enabled"},
      havingValue = "true",
      matchIfMissing = true)
  public ControllerLogAspect controllerLogAspect(
      VeloProperties properties, InvocationLogEngine engine) {
    ControllerLogAspect aspect = new ControllerLogAspect(engine);
    aspect.setOrder(properties.getAspectOrder().getControllerLog());
    return aspect;
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(
      prefix = "velo.log",
      name = {"enabled", "trace.enabled"},
      havingValue = "true",
      matchIfMissing = true)
  public TraceIdFilter traceIdFilter(VeloProperties properties, TraceContextResolver resolver) {
    return new TraceIdFilter(properties, resolver);
  }

  /** 覆盖请求、异步及错误派发，避免后续派发丢失原请求标识。 */
  @Bean
  @ConditionalOnMissingBean(name = "traceIdFilterRegistration")
  @ConditionalOnProperty(
      prefix = "velo.log",
      name = {"enabled", "trace.enabled"},
      havingValue = "true",
      matchIfMissing = true)
  public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration(TraceIdFilter filter) {
    FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setDispatcherTypes(
        DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
    registration.setAsyncSupported(true);
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
    return registration;
  }
}
