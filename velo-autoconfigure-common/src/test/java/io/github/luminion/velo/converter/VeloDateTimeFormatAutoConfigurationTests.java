package io.github.luminion.velo.converter;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.converter.datetime.StringToLocalDateConverter;
import io.github.luminion.velo.converter.datetime.StringToLocalDateTimeConverter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class VeloDateTimeFormatAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    VeloCoreAutoConfiguration.class,
                    VeloDateTimeFormatAutoConfiguration.class
            ));

    @Test
    void shouldRegisterDateTimeConvertersByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(StringToLocalDateConverter.class);
            assertThat(context).hasSingleBean(StringToLocalDateTimeConverter.class);
        });
    }

    @Test
    void shouldRegisterDateTimeConvertersWhenEnabled() {
        contextRunner
                .withPropertyValues("velo.spring-converter.date-time-enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(StringToLocalDateConverter.class);
                    assertThat(context).hasSingleBean(StringToLocalDateTimeConverter.class);
                });
    }

    @Test
    void shouldSkipDateTimeConvertersWhenDisabled() {
        contextRunner
                .withPropertyValues("velo.spring-converter.date-time-enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(StringToLocalDateConverter.class);
                    assertThat(context).doesNotHaveBean(StringToLocalDateTimeConverter.class);
                });
    }
}
