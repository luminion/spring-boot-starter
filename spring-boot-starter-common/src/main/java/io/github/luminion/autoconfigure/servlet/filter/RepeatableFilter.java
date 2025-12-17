package io.github.luminion.autoconfigure.servlet.filter;

import io.github.luminion.autoconfigure.servlet.request.CachedBodyRequestWrapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 * 可重复读过滤器
 *
 * @author luminion
 */
public class RepeatableFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        String ct = request.getContentType();

        // 针对 application/json、text/plain、xml
        boolean bodyRewritable = ct.contains("application/json")
                || ct.contains("text/plain")
                || ct.contains("xml");
        chain.doFilter(bodyRewritable ? new CachedBodyRequestWrapper(request) : request, res);
    }
}
