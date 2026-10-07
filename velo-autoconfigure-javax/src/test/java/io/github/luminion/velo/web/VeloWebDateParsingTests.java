package io.github.luminion.velo.web;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.xss.converter.XssStringConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.format.support.DefaultFormattingConversionService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DateTimeException;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VeloWebDateParsingTests {
    @Test
    void shouldUseShanghaiTimeByDefault() {
        VeloProperties properties = new VeloProperties();
        DefaultFormattingConversionService service = configure(properties);

        assertThat(properties.getDateTimeFormat().getTimeZone()).isEqualTo("Asia/Shanghai");
        assertThat(service.convert("1970-01-01 08:00:00", Date.class)).isEqualTo(new Date(0));
        assertThat(service.convert(new Date(0), String.class)).isEqualTo("1970-01-01 08:00:00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"+08:00", "GMT+8", "GMT+08:00", "Asia/Shanghai"})
    void shouldRespectValidTimeZoneForms(String timeZone) {
        VeloProperties properties = new VeloProperties();
        properties.getDateTimeFormat().setTimeZone(timeZone);

        assertThat(configure(properties).convert("1970-01-01 08:00:00", Date.class)).isEqualTo(new Date(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Wrong/Zone", "+25:00", ""})
    void shouldRejectInvalidTimeZoneDuringRegistration(String timeZone) {
        VeloProperties properties = new VeloProperties();
        properties.getDateTimeFormat().setTimeZone(timeZone);

        assertThatThrownBy(() -> configure(properties)).isInstanceOf(DateTimeException.class);
    }

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

    private DefaultFormattingConversionService configure(VeloProperties properties) {
        DefaultFormattingConversionService service = new DefaultFormattingConversionService();
        new VeloWebMvcConfigurer(new StaticListableBeanFactory().getBeanProvider(XssStringConverter.class), properties)
                .addFormatters(service);
        return service;
    }
}
