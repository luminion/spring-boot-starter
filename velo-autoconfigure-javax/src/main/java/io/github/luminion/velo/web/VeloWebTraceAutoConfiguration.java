package io.github.luminion.velo.web;

import io.github.luminion.velo.VeloProperties.TraceProperties;
import io.github.luminion.velo.log.trace.TraceContextResolver;
import io.github.luminion.velo.log.trace.TraceEnabledCondition;
import io.github.luminion.velo.log.trace.VeloTraceAutoConfiguration;
import javax.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.Ordered;

/** HTTP 请求链路自动配置；独立于 Web 增强和调用日志开关。 */
@AutoConfiguration(after = VeloTraceAutoConfiguration.class)
@ConditionalOnClass(name = "javax.servlet.Filter")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Conditional(TraceEnabledCondition.class)
public class VeloWebTraceAutoConfiguration {
  @Bean
  @ConditionalOnMissingBean
  public TraceIdFilter traceIdFilter(TraceProperties properties, TraceContextResolver resolver) {
    return new TraceIdFilter(properties, resolver);
  }

  /** 覆盖请求、异步及错误派发，避免后续派发丢失原请求标识。 */
  @Bean
  @ConditionalOnMissingBean(name = "traceIdFilterRegistration")
  public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration(TraceIdFilter filter) {
    FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setDispatcherTypes(
        DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
    registration.setAsyncSupported(true);
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
    return registration;
  }
}
