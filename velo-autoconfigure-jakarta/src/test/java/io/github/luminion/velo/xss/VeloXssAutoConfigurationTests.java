package io.github.luminion.velo.xss;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.xss.cleaner.JsoupXssCleaner;
import io.github.luminion.velo.xss.cleaner.SpringHtmlEscapeXssCleaner;
import io.github.luminion.velo.xss.converter.XssStringConverter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class VeloXssAutoConfigurationTests {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    VeloCoreAutoConfiguration.class,
                    VeloXssAutoConfiguration.class
            ));

    @Test
    void shouldCreateCleanerAndStringConverterWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "velo.web.xss.enabled=true"
                )
                .run(context -> {
                    assertThat(context.getBean(XssCleaner.class)).isInstanceOf(JsoupXssCleaner.class);
                    assertThat(context).hasSingleBean(XssStringConverter.class);
                });
    }

    @Test
    void shouldUseSpringEscapeCleanerWithoutJsoup() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("org.jsoup"))
                .withPropertyValues(
                        "velo.web.xss.enabled=true",
                        "velo.web.xss.strategy=ESCAPE"
                )
                .run(context -> {
                    assertThat(context.getBean(XssCleaner.class)).isInstanceOf(SpringHtmlEscapeXssCleaner.class);
                    assertThat(context.getBean(XssCleaner.class).clean("<b>ok</b>")).isEqualTo("&lt;b&gt;ok&lt;/b&gt;");
                    assertThat(context).hasSingleBean(XssStringConverter.class);
                });
    }

    @Test
    void shouldNotSilentlyRegisterFallbackForJsoupStrategy() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("org.jsoup"))
                .withPropertyValues("velo.web.xss.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(XssCleaner.class);
                    assertThat(context).doesNotHaveBean(XssStringConverter.class);
                });
    }
}
