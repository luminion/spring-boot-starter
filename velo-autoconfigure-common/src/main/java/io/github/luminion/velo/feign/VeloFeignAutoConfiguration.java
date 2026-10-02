package io.github.luminion.velo.feign;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogEngine;
import io.github.luminion.velo.log.VeloLogAutoConfiguration;
import io.github.luminion.velo.log.trace.TraceContextResolver;
import org.aspectj.weaver.Advice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Feign 调试日志自动配置。 */
@AutoConfiguration(after = VeloLogAutoConfiguration.class)
@ConditionalOnClass(value = Advice.class, name = "org.springframework.cloud.openfeign.FeignClient")
@ConditionalOnProperty(
    prefix = "velo.feign",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class VeloFeignAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(
      prefix = "velo.log",
      name = {"enabled", "sources.feign.enabled"},
      havingValue = "true",
      matchIfMissing = true)
  public FeignLogAspect feignLogAspect(VeloProperties properties, InvocationLogEngine engine) {
    FeignLogAspect aspect = new FeignLogAspect(engine);
    aspect.setOrder(properties.getAspectOrder().getFeignLog());
    return aspect;
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnClass(name = "feign.RequestInterceptor")
  @ConditionalOnProperty(
      prefix = "velo.log",
      name = {"enabled", "trace.enabled", "trace.feign-propagation-enabled"},
      havingValue = "true",
      matchIfMissing = true)
  public FeignTraceRequestInterceptor feignTraceRequestInterceptor(
      VeloProperties properties, TraceContextResolver resolver) {
    return new FeignTraceRequestInterceptor(properties, resolver);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnClass(name = "feign.Capability")
  @ConditionalOnProperty(
      prefix = "velo.log",
      name = {"enabled", "sources.feign.enabled"},
      havingValue = "true",
      matchIfMissing = true)
  public FeignInvocationCapability feignInvocationCapability() {
    return new FeignInvocationCapability();
  }
}
