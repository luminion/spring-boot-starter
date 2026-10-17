package io.github.luminion.velo.web;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.config.VeloLogAutoConfiguration;
import io.github.luminion.velo.xss.converter.XssStringConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Web MVC 自动配置。
 */
@AutoConfiguration(after = VeloLogAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
        prefix = "velo.web",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class VeloWebAutoConfiguration {

    /** 注册 MVC 增强配置。 */
    @Bean
    @ConditionalOnMissingBean
    public VeloWebMvcConfigurer veloWebMvcConfigurer(
            ObjectProvider<XssStringConverter> xssStringConverterProvider, VeloProperties properties) {
        return new VeloWebMvcConfigurer(xssStringConverterProvider, properties);
    }

    /** 按日志开关注册 Controller 调用日志切面。 */
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
        return new ControllerLogAspect(engine, properties.getAspectOrder().getControllerLog());
    }
}
