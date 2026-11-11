package io.github.luminion.velo.log.config;

import io.github.luminion.velo.core.DeferredInterceptorAdvisor;
import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.log.aspect.InvokeLogAspect;
import io.github.luminion.velo.log.aspect.ScheduledLogAspect;
import io.github.luminion.velo.log.aspect.XxlJobLogAspect;
import io.github.luminion.velo.log.config.condition.ConditionalOnInvocationAdapter;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.trace.TraceScopeManager;
import io.github.luminion.velo.trace.VeloTraceAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.env.Environment;

/**
 * 普通方法和任务入口适配；日志和 trace 独立启停。
 * <p>
 * 拦截器是普通 Bean；Advisor 是只携带切点和顺序的静态基础设施 Bean，拦截器在容器单例初始化完成后才解析，
 * 避免 Spring 注册 BeanPostProcessor 时连带初始化日志引擎、ObjectMapper 等依赖。
 */
@AutoConfiguration(after = {VeloLogAutoConfiguration.class, VeloTraceAutoConfiguration.class})
public class VeloSourceLogAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnInvocationAdapter(source = "annotation")
    public InvokeLogAspect invokeLogAspect(ObjectProvider<InvocationLogEngine> engine,
            ObjectProvider<TraceScopeManager> trace) {
        return new InvokeLogAspect(engine.getIfAvailable(), trace.getIfAvailable());
    }

    @Bean(name = "invokeLogAdvisor")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(name = "invokeLogAdvisor")
    @ConditionalOnBean(InvokeLogAspect.class)
    public static DeferredInterceptorAdvisor invokeLogAdvisor(Environment environment,
            ObjectProvider<InvokeLogAspect> invokeLogAspect) {
        int order = Binder.get(environment).bind("velo.aspect-order.invoke-log", Integer.class)
                .orElse(VeloAdvisorOrder.LOG_INVOKE);
        return new DeferredInterceptorAdvisor(InvokeLogAspect.pointcut(), order, invokeLogAspect::getObject);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnInvocationAdapter(source = "scheduled")
    public ScheduledLogAspect scheduledLogAspect(ObjectProvider<InvocationLogEngine> engine,
            ObjectProvider<TraceScopeManager> trace) {
        return new ScheduledLogAspect(engine.getIfAvailable(), trace.getIfAvailable());
    }

    @Bean(name = "scheduledLogAdvisor")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(name = "scheduledLogAdvisor")
    @ConditionalOnBean(ScheduledLogAspect.class)
    public static DeferredInterceptorAdvisor scheduledLogAdvisor(ObjectProvider<ScheduledLogAspect> scheduledLogAspect) {
        return new DeferredInterceptorAdvisor(
                ScheduledLogAspect.pointcut(), VeloAdvisorOrder.LOG_CONTROLLER, scheduledLogAspect::getObject);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "com.xxl.job.core.handler.annotation.XxlJob")
    @ConditionalOnInvocationAdapter(source = "xxl-job")
    public XxlJobLogAspect xxlJobLogAspect(ObjectProvider<InvocationLogEngine> engine,
            ObjectProvider<TraceScopeManager> trace) {
        return new XxlJobLogAspect(engine.getIfAvailable(), trace.getIfAvailable());
    }

    @Bean(name = "xxlJobLogAdvisor")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(name = "xxlJobLogAdvisor")
    @ConditionalOnBean(XxlJobLogAspect.class)
    public static DeferredInterceptorAdvisor xxlJobLogAdvisor(ObjectProvider<XxlJobLogAspect> xxlJobLogAspect) {
        return new DeferredInterceptorAdvisor(
                XxlJobLogAspect.pointcut(), VeloAdvisorOrder.LOG_CONTROLLER, xxlJobLogAspect::getObject);
    }
}
