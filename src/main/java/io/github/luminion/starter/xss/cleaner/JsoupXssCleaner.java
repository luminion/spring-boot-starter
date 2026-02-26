package io.github.luminion.starter.xss.cleaner;

import io.github.luminion.starter.xss.XssCleaner;
import io.github.luminion.starter.xss.XssStrategy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.jsoup.safety.Safelist;
import org.springframework.web.util.HtmlUtils;

/**
 * 基于 Jsoup 的 XSS 清理器
 *
 * @author luminion
 * @since 1.0.0
 */
public class JsoupXssCleaner implements XssCleaner {

    private final XssStrategy strategy;
    private final Safelist safelist;
    private final Document.OutputSettings outputSettings;

    public JsoupXssCleaner(XssStrategy strategy) {
        this.strategy = strategy;

        // 1. 如果是不需要 Jsoup 的策略，直接赋 null 节省内存
        if (strategy == XssStrategy.NONE || strategy == XssStrategy.ESCAPE) {
            this.safelist = null;
            this.outputSettings = null;
        } else {
            // 2. 预热 Jsoup 配置
            if (strategy == XssStrategy.SIMPLE_TEXT){
                this.safelist = Safelist.simpleText();
            }else if (strategy == XssStrategy.BASIC){
                this.safelist = Safelist.basic();
            }else if (strategy == XssStrategy.BASIC_WITH_IMAGES){
                this.safelist = Safelist.basicWithImages();
            }else {
                this.safelist = Safelist.relaxed();
            }
            this.outputSettings = new Document.OutputSettings()
                    .prettyPrint(false)
                    .escapeMode(Entities.EscapeMode.xhtml);
        }
    }

    @Override
    public String clean(String html) {
        // 1. 判空与放行策略
        if (html == null || html.isEmpty() || strategy == XssStrategy.NONE) {
            return html;
        }

        // 2. 转义策略：直接交由 Spring 底层的高性能处理，不需要前置判断
        if (strategy == XssStrategy.ESCAPE) {
            return HtmlUtils.htmlEscape(html);
        }

        // 3. Jsoup 清理策略：只有包含 HTML 标签的标志性字符（< 或 &）时，才触发重量级 Jsoup 引擎
        if (!html.contains("<") && !html.contains("&")) {
            return html;
        }

        return Jsoup.clean(html, "", safelist, outputSettings);
    }
}