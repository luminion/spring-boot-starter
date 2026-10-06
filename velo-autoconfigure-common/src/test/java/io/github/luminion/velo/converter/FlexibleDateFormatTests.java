package io.github.luminion.velo.converter;

import io.github.luminion.velo.converter.datetime.FlexibleDateFormat;
import org.junit.jupiter.api.Test;
import java.text.ParseException;
import java.util.TimeZone;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlexibleDateFormatTests {
    @Test
    void shouldRejectInvalidDateByDefaultAndRespectExplicitLeniency() throws Exception {
        FlexibleDateFormat format = new FlexibleDateFormat("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", TimeZone.getTimeZone("UTC"));
        assertThatThrownBy(() -> format.parse("2026-02-30")).isInstanceOf(ParseException.class);
        assertThatThrownBy(() -> format.parse("2026-02-30 12:00:00")).isInstanceOf(ParseException.class);
        assertThat(format.format(format.parse("2024-02-29"))).isEqualTo("2024-02-29 00:00:00");
        format.setLenient(true);
        assertThat(format.format(format.parse("2026-02-30"))).isEqualTo("2026-03-02 00:00:00");
    }
}
