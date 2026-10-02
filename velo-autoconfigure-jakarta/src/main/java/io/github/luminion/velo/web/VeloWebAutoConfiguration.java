package io.github.luminion.velo.web;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogEngine;
import io.github.luminion.velo.log.VeloLogAutoConfiguration;
import io.github.luminion.velo.xss.converter.XssStringConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

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
}
