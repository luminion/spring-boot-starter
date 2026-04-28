package io.github.luminion.velo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.logging.LogLevel;

import static org.assertj.core.api.Assertions.assertThat;

class VeloPropertiesDefaultsTest {

    @Test
    void shouldUseProductionSafeDefaultsForLogging() {
        VeloProperties properties = new VeloProperties();

        assertThat(properties.getLog().getLevel()).isEqualTo(LogLevel.INFO);
        assertThat(properties.getDateTimeFormat().getConverters().isEnabled()).isFalse();
        assertThat(properties.getExcel().getConverters().isEnabled()).isFalse();
        assertThat(properties.getExcel().isEnabled()).isTrue();
        assertThat(properties.getJackson().isWriteUnsafeIntegerAsString()).isTrue();
        assertThat(properties.getJackson().isWriteBigDecimalAsString()).isTrue();
        assertThat(properties.getJackson().isWriteFloatingPointAsString()).isFalse();
        assertThat(properties.getJackson().getDateTime().isEnabled()).isTrue();
        assertThat(properties.getJackson().getStringConverters().isEnabled()).isFalse();
        assertThat(properties.getCore().getEnumCodeFields()).containsExactly("code", "id", "value");
        assertThat(properties.getCore().getEnumDescFields()).containsExactly("desc", "name", "label");
        assertThat(properties.getIdempotent().getBackend()).isEqualTo(ConcurrencyBackend.AUTO);
        assertThat(properties.getIdempotent().getKeyPrefix()).isEqualTo("idempotent:");
        assertThat(properties.getRateLimit().getBackend()).isEqualTo(ConcurrencyBackend.AUTO);
        assertThat(properties.getRateLimit().getKeyPrefix()).isEqualTo("rateLimit:");
        assertThat(properties.getLock().getBackend()).isEqualTo(ConcurrencyBackend.AUTO);
        assertThat(properties.getLock().getKeyPrefix()).isEqualTo("lock:");
        assertThat(properties.getLog().isEnabled()).isTrue();
        assertThat(properties.getWeb().isEnabled()).isTrue();
        assertThat(properties.getWeb().getRequestLogging().isEnabled()).isFalse();
    }
}
