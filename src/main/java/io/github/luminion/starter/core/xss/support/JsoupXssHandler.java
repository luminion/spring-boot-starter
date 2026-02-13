package io.github.luminion.starter.core.xss.support;

import io.github.luminion.starter.core.spi.XssHandler;
import io.github.luminion.starter.core.xss.XssStrategy;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class JsoupXssHandler implements XssHandler {

    private final XssStrategy strategy;

    @Override
    public String handle(String html) {
        if (html == null || strategy == XssStrategy.NONE) {
            return html;
        }

        if (strategy == XssStrategy.ESCAPE) {
            return HtmlUtils.htmlEscape(html);
        }

        Safelist safelist = switch (strategy) {
            case SIMPLE_TEXT -> Safelist.simpleText();
            case BASIC -> Safelist.basic();
            case BASIC_WITH_IMAGES -> Safelist.basicWithImages();
//            case RELAXED -> Safelist.relaxed();
            default -> Safelist.relaxed();
        };

        // 保持实体不转义 (如 &nbsp; 保持原样)
        Document.OutputSettings outputSettings = new Document.OutputSettings()
                .prettyPrint(false)
                .escapeMode(Entities.EscapeMode.xhtml);

        return Jsoup.clean(html, "", safelist, outputSettings);
    }

    @Override
    public boolean isXss(String html) {
        if (html == null || strategy == XssStrategy.NONE) {
            return false;
        }
        return !handle(html).equals(html);
    }
}
