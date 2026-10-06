package io.github.luminion.velo.web;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.xss.converter.XssStringConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.format.support.DefaultFormattingConversionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VeloWebDateParsingTests {
    @Test
    void shouldRejectInvalidDatesInRegisteredMvcFormatters() {
        DefaultFormattingConversionService service = new DefaultFormattingConversionService();
        VeloWebMvcConfigurer configurer = new VeloWebMvcConfigurer(
                new StaticListableBeanFactory().getBeanProvider(XssStringConverter.class), new VeloProperties());
        configurer.addFormatters(service);
        assertThatThrownBy(() -> service.convert("2026-02-30", LocalDate.class))
                .isInstanceOf(ConversionFailedException.class);
        assertThatThrownBy(() -> service.convert("2026-02-30 12:00:00", LocalDateTime.class))
                .isInstanceOf(ConversionFailedException.class);
        assertThatThrownBy(() -> service.convert("2026-02-30", Date.class))
                .isInstanceOf(ConversionFailedException.class);
        assertThat(service.convert("2024-02-29", LocalDate.class)).isEqualTo(LocalDate.of(2024, 2, 29));
    }
}
