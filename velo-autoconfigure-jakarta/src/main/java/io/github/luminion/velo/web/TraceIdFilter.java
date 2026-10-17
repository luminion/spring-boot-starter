package io.github.luminion.velo.web;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.VeloProperties.TraceProperties;
import io.github.luminion.velo.trace.TraceContext;
import io.github.luminion.velo.trace.TraceContextResolver;
import io.github.luminion.velo.trace.TraceData;
import io.github.luminion.velo.trace.W3cTraceContextResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 每次 HTTP 请求建立 traceId；异步或异常再派发复用请求属性中的同一标识。
 */
public class TraceIdFilter extends OncePerRequestFilter {
    private static final String ATTRIBUTE = TraceIdFilter.class.getName() + ".traceId";
    private final TraceProperties properties;
    private final TraceContextResolver resolver;

    public TraceIdFilter(VeloProperties properties) {
        this(properties, new W3cTraceContextResolver());
    }

    public TraceIdFilter(VeloProperties properties, TraceContextResolver resolver) {
        this(properties.getLog().getTrace(), resolver);
    }

    public TraceIdFilter(TraceProperties properties, TraceContextResolver resolver) {
        this.properties = properties;
        this.resolver = resolver;
        String headerName = properties.getResponseHeaderName();
        if (properties.isEnabled() && properties.isResponseHeaderEnabled()
                && (!TraceData.validHeaderName(headerName)
                || "traceparent".equalsIgnoreCase(headerName)
                || "tracestate".equalsIgnoreCase(headerName))) {
            throw new IllegalArgumentException(
                    "velo.trace.response-header-name must be a valid HTTP header name other than traceparent/tracestate");
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        TraceProperties trace = properties;
        if (!trace.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }
        Object captured = request.getAttribute(ATTRIBUTE);
        TraceData data = captured instanceof TraceData ? (TraceData) captured : null;
        if (data == null) {
            data = resolve(request, response, trace.getMdcKey());
            request.setAttribute(ATTRIBUTE, data);
        }
        try (TraceContext.Scope scope = TraceContext.install(trace.getMdcKey(), data, true)) {
            // 始终回传入口快照中的纯 traceId，不重新解析请求头或读取可能变化的 MDC。
            if (trace.isResponseHeaderEnabled() && !response.isCommitted()) {
                response.setHeader(trace.getResponseHeaderName(), data.getTraceId());
            }
            chain.doFilter(request, response);
        }
    }

    @Override
    protected void doFilterNestedErrorDispatch(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 容器在原派发未结束时进入错误页，响应可能已重置，重新写入同一个请求标识。
        doFilterInternal(request, response, chain);
    }

    private TraceData resolve(HttpServletRequest request, HttpServletResponse response, String key) {
        RequestAttributes previous = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes current = new ServletRequestAttributes(request, response);
        try {
            // Filter 可能早于 Spring 的请求上下文绑定；仅在解析期间临时建立并随后恢复。
            RequestContextHolder.setRequestAttributes(current);
            return TraceContext.resolveInbound(key, resolver);
        } finally {
            try {
                current.requestCompleted();
            } finally {
                if (previous == null) {
                    RequestContextHolder.resetRequestAttributes();
                } else {
                    RequestContextHolder.setRequestAttributes(previous);
                }
            }
        }
    }
}
