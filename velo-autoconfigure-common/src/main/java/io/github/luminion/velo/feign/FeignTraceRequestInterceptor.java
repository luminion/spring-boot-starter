package io.github.luminion.velo.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.trace.TraceContext;
import io.github.luminion.velo.log.trace.TraceContextResolver;
import io.github.luminion.velo.log.trace.W3cTraceContextResolver;

/** 将当前 traceId 传播到 Feign 请求，替换同名请求头以避免重复。 */
public class FeignTraceRequestInterceptor implements RequestInterceptor {

  private final VeloProperties properties;
  private final TraceContextResolver resolver;

  public FeignTraceRequestInterceptor(VeloProperties properties) {
    this(properties, new W3cTraceContextResolver());
  }

  public FeignTraceRequestInterceptor(VeloProperties properties, TraceContextResolver resolver) {
    this.properties = properties;
    this.resolver = resolver;
  }

  @Override
  public void apply(RequestTemplate template) {
    VeloProperties.TraceProperties trace = properties.getLog().getTrace();
    if (!properties.getLog().isEnabled()
        || !trace.isEnabled()
        || !trace.isFeignPropagationEnabled()) {
      return;
    }
    try (TraceContext.Scope scope = TraceContext.open(trace.getMdcKey(), true, resolver)) {
      TraceContext.current()
          .getPropagationHeaders()
          .forEach(
              (name, value) -> {
                template.removeHeader(name);
                if (!value.isEmpty()) {
                  template.header(name, value);
                }
              });
    }
  }
}
