package io.github.luminion.velo.xss;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.xss.cleaner.JsoupXssCleaner;
import io.github.luminion.velo.xss.converter.XssStringConverter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
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
                        "velo.web.xss.enabled=true",
                        "velo.web.xss.string-converter-enabled=true"
                )
                .run(context -> {
                    assertThat(context.getBean(XssCleaner.class)).isInstanceOf(JsoupXssCleaner.class);
                    assertThat(context).hasSingleBean(XssStringConverter.class);
                });
    }
}
