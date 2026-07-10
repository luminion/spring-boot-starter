package io.github.luminion.velo.web;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.trace.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        // 保存上游可能已写入的 MDC 值，请求结束后还原而非直接清除，避免破坏外部 tracing 上下文
        String previous = TraceContext.get(trace.getMdcKey());
        // 校验入站 traceId，非法(超长/含控制字符等)则重新生成，防止日志污染或注入
        String traceId = TraceContext.resolveInbound(request.getHeader(trace.getHeaderName()));
        TraceContext.put(trace.getMdcKey(), traceId);
        if (trace.isResponseHeaderEnabled()) {
            response.setHeader(trace.getHeaderName(), traceId);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            TraceContext.restore(trace.getMdcKey(), previous);
        }
    }
}
