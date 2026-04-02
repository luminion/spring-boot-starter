package io.github.luminion.velo.excel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class VeloExcelAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VeloExcelAutoConfiguration.class));

    @Test
    void shouldStartWhenExcelAutoRegistrationIsEnabled() {
        contextRunner
                .withPropertyValues(
                        "velo.date-time-format.date=dd/MM/yyyy",
                        "velo.date-time-format.time=HH:mm",
                        "velo.date-time-format.date-time=dd/MM/yyyy HH:mm",
                        "velo.date-time-format.time-zone=UTC",
                        "velo.excel.converters.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                });
    }
}
