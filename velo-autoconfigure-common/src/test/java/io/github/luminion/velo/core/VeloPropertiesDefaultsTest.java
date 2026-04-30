package io.github.luminion.velo;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.logging.LogLevel;

import static org.assertj.core.api.Assertions.assertThat;

class VeloPropertiesDefaultsTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VeloCoreAutoConfiguration.class));

    @Test
    void shouldUseProductionSafeDefaultsForLogging() {
        VeloProperties properties = new VeloProperties();

        assertThat(properties.getLog().getLevel()).isEqualTo(LogLevel.INFO);
        assertThat(properties.getSpringConverter().isDateTimeEnabled()).isTrue();
        assertThat(properties.getExcel().getConverters().isEnabled()).isTrue();
        assertThat(properties.getExcel().isEnabled()).isTrue();
        assertThat(properties.getJackson().isUnsafeIntegerAsString()).isTrue();
        assertThat(properties.getJackson().isBigDecimalAsString()).isTrue();
        assertThat(properties.getJackson().isFloatingAsString()).isFalse();
        assertThat(properties.getJackson().isDateTimeEnabled()).isTrue();
        assertThat(properties.getJackson().isEnumDescEnabled()).isTrue();
        assertThat(properties.getJackson().isStringConverterEnabled()).isTrue();
        assertThat(properties.getJackson().getEnumNameSuffix()).isEqualTo("name");
        assertThat(properties.getJackson().getEnumMappings())
                .containsEntry("code", "name")
                .containsEntry("key", "value");
        assertThat(properties.getIdempotent().getBackend()).isEqualTo(ConcurrencyBackend.AUTO);
        assertThat(properties.getIdempotent().getPrefix()).isEqualTo("idempotent:");
        assertThat(properties.getRateLimit().getBackend()).isEqualTo(ConcurrencyBackend.AUTO);
        assertThat(properties.getRateLimit().getPrefix()).isEqualTo("rateLimit:");
        assertThat(properties.getLock().getBackend()).isEqualTo(ConcurrencyBackend.AUTO);
        assertThat(properties.getLock().getPrefix()).isEqualTo("lock:");
        assertThat(properties.getLog().isEnabled()).isTrue();
        assertThat(properties.getWeb().isEnabled()).isTrue();
        assertThat(properties.getWeb().isRequestLoggingEnabled()).isTrue();
    }

    @Test
    void shouldMergeDefaultEnumMappingsWhenConfigured() {
        contextRunner
                .withPropertyValues(
                        "velo.jackson.enum-mappings.key=name",
                        "velo.jackson.enum-mappings.value=desc")
                .run(context -> {
                    VeloProperties properties = context.getBean(VeloProperties.class);

                    assertThat(properties.getJackson().getEnumMappings())
                            .containsEntry("code", "name")
                            .containsEntry("key", "name")
                            .containsEntry("value", "desc");
                });
    }
}
