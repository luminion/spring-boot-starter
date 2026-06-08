package io.github.luminion.velo.web;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.trace.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Creates and exposes trace id for servlet requests.
 */
public class TraceIdFilter extends OncePerRequestFilter {

    private final VeloProperties properties;

    public TraceIdFilter(VeloProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        VeloProperties.TraceProperties trace = properties.getLog().getTrace();
        String traceId = request.getHeader(trace.getHeaderName());
        if (!StringUtils.hasText(traceId)) {
            traceId = TraceContext.createTraceId();
        }
        TraceContext.put(trace.getMdcKey(), traceId);
        if (trace.isResponseHeaderEnabled()) {
            response.setHeader(trace.getHeaderName(), traceId);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            TraceContext.remove(trace.getMdcKey());
        }
    }
}
