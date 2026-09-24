package io.github.luminion.velo.xss.cleaner;

import io.github.luminion.velo.xss.XssStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.HtmlUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JsoupXssCleanerTests {

    @Test
    void shouldKeepPlainTextWithAmpersandUntouched() {
        JsoupXssCleaner cleaner = new JsoupXssCleaner(XssStrategy.BASIC);

        // 含 & 但无 HTML 标签的普通文本不得被实体化改写为 &amp; 后入库
        assertThat(cleaner.clean("AT&T")).isEqualTo("AT&T");
        assertThat(cleaner.clean("张三&李四")).isEqualTo("张三&李四");
        assertThat(cleaner.clean("a=1&b=2")).isEqualTo("a=1&b=2");
    }

    @Test
    void shouldKeepPreEscapedEntitiesUntouched() {
        JsoupXssCleaner cleaner = new JsoupXssCleaner(XssStrategy.BASIC);

        // 无真实标签的预转义文本原样放行：实体在浏览器中只展示不执行，无注入风险
        assertThat(cleaner.clean("&lt;img src=x onerror=alert(1)&gt;"))
                .isEqualTo("&lt;img src=x onerror=alert(1)&gt;");
    }

    @Test
    void shouldCleanHtmlWhenTagMarkerPresent() {
        JsoupXssCleaner cleaner = new JsoupXssCleaner(XssStrategy.BASIC);

        assertThat(cleaner.clean("<script>alert(1)</script>")).doesNotContain("script");
        assertThat(cleaner.clean("<b>ok</b>")).isEqualTo("<b>ok</b>");
    }

    @Test
    void shouldPreserveRelativeLinksAndStripUnsafeProtocols() {
        JsoupXssCleaner cleaner = new JsoupXssCleaner(XssStrategy.BASIC);

        String cleaned = cleaner.clean(
                "<p>see <a href=\"/detail?id=1\">detail</a> and <a href=\"https://example.com/abs\">abs</a>"
                        + " and <a href=\"javascript:alert(1)\">evil</a></p>");

        // 相对链接原样保留，未被剥离或改写为绝对地址
        assertThat(cleaned).contains("<a href=\"/detail?id=1\">");
        assertThat(cleaned).contains("<a href=\"https://example.com/abs\"");
        assertThat(cleaned).doesNotContain("javascript:");
    }

    @Test
    void shouldPreserveRelativeImageSource() {
        JsoupXssCleaner cleaner = new JsoupXssCleaner(XssStrategy.BASIC_WITH_IMAGES);

        String cleaned = cleaner.clean("<img src=\"/img/a.png\" alt=\"a\">");

        assertThat(cleaned).contains("src=\"/img/a.png\"");
    }

    @Test
    void shouldEscapeWithHtmlUtilsUnderEscapeStrategy() {
        JsoupXssCleaner cleaner = new JsoupXssCleaner(XssStrategy.ESCAPE);

        assertThat(cleaner.clean("AT&T")).isEqualTo(HtmlUtils.htmlEscape("AT&T"));
    }

    @Test
    void shouldBypassNoneStrategy() {
        JsoupXssCleaner cleaner = new JsoupXssCleaner(XssStrategy.NONE);

        assertThat(cleaner.clean("<script>alert(1)</script>")).isEqualTo("<script>alert(1)</script>");
    }

    @Test
    void shouldPassThroughNullAndEmpty() {
        JsoupXssCleaner cleaner = new JsoupXssCleaner(XssStrategy.BASIC);

        assertThat(cleaner.clean(null)).isNull();
        assertThat(cleaner.clean("")).isEmpty();
    }
}
