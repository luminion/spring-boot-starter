package io.github.luminion.velo.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.trace.TraceContext;
import org.springframework.util.StringUtils;

/**
 * Propagates current trace id to Feign requests.
 */
public class FeignTraceRequestInterceptor implements RequestInterceptor {

    private final VeloProperties properties;

    public FeignTraceRequestInterceptor(VeloProperties properties) {
        this.properties = properties;
    }

    @Override
    public void apply(RequestTemplate template) {
        VeloProperties.TraceProperties trace = properties.getLog().getTrace();
        String traceId = TraceContext.get(trace.getMdcKey());
        if (!StringUtils.hasText(traceId)) {
            traceId = TraceContext.createTraceId();
        }
        template.header(trace.getHeaderName(), traceId);
    }
}
