package io.github.luminion.velo.log;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.aspect.InvokeLogAspect;
import io.github.luminion.velo.log.support.Slf4JInvocationLogWriter;
import io.github.luminion.velo.log.trace.MdcTaskDecorator;
import io.github.luminion.velo.log.trace.TraceContextResolver;
import io.github.luminion.velo.log.trace.W3cTraceContextResolver;
import org.aspectj.weaver.Advice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;

/** 共用日志引擎、对象转换和默认线程池 MDC 传播。 */
@AutoConfiguration(afterName = "io.github.luminion.velo.jackson.VeloJacksonLogAutoConfiguration")
@ConditionalOnClass(Advice.class)
@ConditionalOnProperty(
    prefix = "velo.log",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class VeloLogAutoConfiguration {
  @Bean
  @ConditionalOnMissingBean(TraceContextResolver.class)
  public TraceContextResolver traceContextResolver() {
    return new W3cTraceContextResolver();
  }

  @Bean
  @ConditionalOnMissingBean(TaskDecorator.class)
  @ConditionalOnProperty(
      prefix = "velo.log.trace",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public TaskDecorator mdcTaskDecorator(VeloProperties properties, TraceContextResolver resolver) {
    return new MdcTaskDecorator(properties.getLog().getTrace().getMdcKey(), resolver);
  }

  @Bean
  @ConditionalOnMissingBean
  public LogValueFormatter logValueFormatter() {
    return String::valueOf;
  }

  @Bean
  @ConditionalOnMissingBean
  public InvocationLogWriter invocationLogWriter() {
    return new Slf4JInvocationLogWriter();
  }

  @Bean
  @ConditionalOnMissingBean
  public InvocationLogEngine invocationLogEngine(
      VeloProperties properties,
      LogValueFormatter formatter,
      InvocationLogWriter writer,
      TraceContextResolver resolver) {
    validateThreshold(properties.getLog().getDefaults().getSlowLog());
    VeloProperties.InvocationSources sources = properties.getLog().getSources();
    validateThreshold(sources.getController().getSlowLog());
    validateThreshold(sources.getFeign().getSlowLog());
    validateThreshold(sources.getInvoke().getSlowLog());
    validateThreshold(sources.getScheduled().getSlowLog());
    validateThreshold(sources.getXxlJob().getSlowLog());
    return new InvocationLogEngine(properties, formatter, writer, resolver);
  }

  @Bean
  @ConditionalOnMissingBean
  public InvokeLogAspect invokeLogAspect(VeloProperties properties, InvocationLogEngine engine) {
    InvokeLogAspect aspect = new InvokeLogAspect(engine);
    aspect.setOrder(properties.getAspectOrder().getInvokeLog());
    return aspect;
  }

  private void validateThreshold(VeloProperties.SlowLogProperties value) {
    if (value.getThresholdMs() != null && value.getThresholdMs() < 0) {
      throw new IllegalArgumentException("SlowLog threshold-ms must be zero or greater");
    }
  }
}
