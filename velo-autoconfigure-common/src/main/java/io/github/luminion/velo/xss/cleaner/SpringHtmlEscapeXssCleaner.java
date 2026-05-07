package io.github.luminion.velo.xss.cleaner;

import io.github.luminion.velo.xss.XssCleaner;
import org.springframework.web.util.HtmlUtils;

/**
 * 基于 Spring HtmlUtils 的 XSS 转义器。
 */
public class SpringHtmlEscapeXssCleaner implements XssCleaner {

    @Override
    public String clean(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        return HtmlUtils.htmlEscape(html);
    }
}
