package io.github.luminion.autoconfigure.servlet.filter;

import io.github.luminion.autoconfigure.servlet.request.CachedBodyRequestWrapper;
import io.github.luminion.autoconfigure.servlet.request.XssRequestWrapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

/**
 * xss过滤器
 * @author luminion
 */
@RequiredArgsConstructor
public class XssFilter implements Filter {
    /**
     * 匹配器
     */
    protected final PathMatcher matcher = new AntPathMatcher();
    /**
     * 包含链接
     */
    @Getter
    protected final Collection<String> includes;
    /**
     * 排除链接
     */
    @Getter
    protected final Collection<String> excludes;
    /**
     * xss过滤方法
     */
    protected final Function<String, String> sanitizer;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        if (!isMatchedURL(request)){
            chain.doFilter(request, res);
            return;
        }

        String ct = Optional.ofNullable(request.getContentType()).orElse("");
        boolean bodyRewritable = ct.contains("application/json")
                || ct.contains("text/plain")
                || ct.contains("xml");
        byte[] bodyBytes = null;
        if (bodyRewritable) {
            // 读原始体
            bodyBytes = readAll(request.getInputStream());
            String enc = Optional.ofNullable(request.getCharacterEncoding())
                    .orElse(StandardCharsets.UTF_8.name());
            String body = new String(bodyBytes, enc);
            String sanitized = sanitizeBody(body, ct); // 实现你自己的 JSON/XML 安全净化
            bodyBytes = sanitized.getBytes(enc);
        }

        HttpServletRequest wrapped =
                bodyBytes != null
                        ? new CachedBodyRequestWrapper(request, bodyBytes)
//                        : new CachedBodyRequestWrapper(request) // 仅缓存，不修改
                        : request // 节约内存
                ;

        XssRequestWrapper xssWrapper = new XssRequestWrapper(wrapped, sanitizer);
        chain.doFilter(xssWrapper, res);
    }



    public boolean isMatchedURL(HttpServletRequest request) {
        return !this.isExcludeURL(request) && this.isIncludeURL(request);
    }

    public boolean isIncludeURL(HttpServletRequest request) {
        String url = request.getServletPath();
        // 如果没有包含规则，则默认不过滤
        if (includes == null || includes.isEmpty()) {
            return false;
        }
        // 检查是否匹配任意包含模式
        for (String pattern : includes) {
            if (matcher.match(pattern, url)) {
                return true;
            }
        }
        return false;
    }

    public boolean isExcludeURL(HttpServletRequest request) {
        String url = request.getServletPath();
        if (url == null || url.isEmpty()) {
            return false;
        }
        if (excludes == null || excludes.isEmpty()) {
            return false;
        }
        for (String pattern : excludes) {
            if (matcher.match(pattern, url)) {
                return true;
            }
        }
        return false;
    }


    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }

    private String sanitizeBody(String body, String contentType) {
        // 注意：不要破坏 JSON 结构。可以仅转义字符串字段中的危险字符，或使用 JSON-aware 的转义。
        return sanitizer.apply(body);
    }

}
