package io.github.luminion.autoconfigure.servlet.filter;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Collection;

/**
 * 防盗链过滤器
 * 参考自RuoYi
 *
 * @author luminion
 */
@RequiredArgsConstructor
public class RefererFilter implements Filter {
    private final Collection<String> allowDomains;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String referer = req.getHeader("Referer");

        // 如果Referer为空，拒绝访问
        if (referer == null || referer.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: Referer header is required");
            return;
        }

        // 检查Referer是否在允许的域名列表中
        boolean allowed = false;
        for (String domain : allowDomains) {
            if (referer.contains(domain)) {
                allowed = true;
                break;
            }
        }

        // 根据检查结果决定是否放行
        if (allowed) {
            chain.doFilter(request, response);
        } else {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: Referer '" + referer + "' is not allowed");
        }
    }

}
