package io.github.luminion.starter.web.filter;

import io.github.luminion.starter.web.request.CachedBodyRequestWrapper;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 可重复读过滤器（jakarta版本）
 *
 * @author luminion
 */
public class RepeatableFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        // 修复：使用Optional处理可能为null的ContentType，避免NullPointerException
        String ct = request.getContentType();
        
        // 针对 application/json、text/plain、xml
        // 只有当ContentType不为null且包含指定类型时才包装请求体
        boolean bodyRewritable = ct != null && (
                ct.contains("application/json")
                || ct.contains("text/plain")
                || ct.contains("xml")
        );
        chain.doFilter(bodyRewritable ? new CachedBodyRequestWrapper(request) : request, res);
    }
}

