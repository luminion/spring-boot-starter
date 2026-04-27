package io.github.luminion.velo.core;

import org.junit.jupiter.api.Test;
import org.springframework.boot.logging.LogLevel;

import static org.assertj.core.api.Assertions.assertThat;

class VeloPropertiesDefaultsTest {

    @Test
    void shouldUseProductionSafeDefaultsForLogging() {
        VeloProperties properties = new VeloProperties();

        assertThat(properties.getLogLevel()).isEqualTo(LogLevel.INFO);
        assertThat(properties.getDateTimeFormat().getConverters().isEnabled()).isFalse();
        assertThat(properties.getExcel().getConverters().isEnabled()).isFalse();
        assertThat(properties.getExcel().isEnabled()).isTrue();
        assertThat(properties.getJackson().isWriteUnsafeIntegerAsString()).isTrue();
        assertThat(properties.getJackson().isWriteBigDecimalAsString()).isTrue();
        assertThat(properties.getJackson().isWriteFloatingPointAsString()).isFalse();
        assertThat(properties.getJackson().isBuilderCustomizerEnabled()).isTrue();
        assertThat(properties.getJackson().getDateTime().isEnabled()).isTrue();
        assertThat(properties.getJackson().getDateTime().isJavaUtilDateEnabled()).isTrue();
        assertThat(properties.getJackson().getDateTime().isSerializersEnabled()).isTrue();
        assertThat(properties.getJackson().getDateTime().isDeserializersEnabled()).isTrue();
        assertThat(properties.getJackson().getStringConverters().isEnabled()).isFalse();
        assertThat(properties.getCore().isEnabled()).isTrue();
        assertThat(properties.getIdempotent().getBackend()).isEqualTo(ConcurrencyBackend.AUTO);
        assertThat(properties.getRateLimit().getBackend()).isEqualTo(ConcurrencyBackend.AUTO);
        assertThat(properties.getLock().getBackend()).isEqualTo(ConcurrencyBackend.AUTO);
        assertThat(properties.getLog().isEnabled()).isTrue();
        assertThat(properties.getLog().isSlf4jLogWriterEnabled()).isTrue();
        assertThat(properties.getWeb().isEnabled()).isTrue();
        assertThat(properties.getWeb().isMvcConfigurerEnabled()).isTrue();
        assertThat(properties.getWeb().isDateTimeFormatterRegistrationEnabled()).isTrue();
        assertThat(properties.getWeb().isXssStringConverterRegistrationEnabled()).isTrue();
        assertThat(properties.getWeb().getRequestLogging().isEnabled()).isFalse();
    }
}
