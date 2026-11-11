package io.github.luminion.velo.web;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.DeferredInterceptorAdvisor;
import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.config.VeloLogAutoConfiguration;
import io.github.luminion.velo.xss.converter.XssStringConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.env.Environment;

/**
 * 注册 Web MVC 增强配置和控制器调用日志切面。
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

    @Bean
    @ConditionalOnMissingBean
    public VeloWebMvcConfigurer veloWebMvcConfigurer(
            ObjectProvider<XssStringConverter> xssStringConverterProvider,
            VeloProperties properties) {
        return new VeloWebMvcConfigurer(xssStringConverterProvider, properties);
    }

    /**
     * 根据日志配置注册 Controller 调用日志拦截器。
     *
     * @param engine 调用日志引擎
     * @return Controller 调用日志拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "velo.log",
            name = {"enabled", "sources.controller.enabled"},
            havingValue = "true",
            matchIfMissing = true)
    public ControllerLogAspect controllerLogAspect(InvocationLogEngine engine) {
        return new ControllerLogAspect(engine);
    }

    /**
     * 只携带切点和顺序的基础设施 Advisor；拦截器在容器单例初始化完成后解析，避免提前创建业务依赖。
     */
    @Bean(name = "controllerLogAdvisor")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(name = "controllerLogAdvisor")
    @ConditionalOnBean(ControllerLogAspect.class)
    public static DeferredInterceptorAdvisor controllerLogAdvisor(Environment environment,
            ObjectProvider<ControllerLogAspect> controllerLogAspect) {
        int order = Binder.get(environment).bind("velo.aspect-order.controller-log", Integer.class)
                .orElse(VeloAdvisorOrder.LOG_CONTROLLER);
        return new DeferredInterceptorAdvisor(ControllerLogAspect.pointcut(), order, controllerLogAspect::getObject);
    }
}
