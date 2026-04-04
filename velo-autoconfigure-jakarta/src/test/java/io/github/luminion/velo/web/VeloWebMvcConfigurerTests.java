package io.github.luminion.velo.web;

import io.github.luminion.velo.core.VeloProperties;
import io.github.luminion.velo.xss.converter.XssStringConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.format.support.DefaultFormattingConversionService;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VeloWebMvcConfigurerTests {

    private static ObjectProvider<XssStringConverter> emptyConverterProvider() {
        return new StaticListableBeanFactory().getBeanProvider(XssStringConverter.class);
    }

    @Test
    void shouldRegisterCustomDateFormattersWhenEnabled() {
        VeloProperties properties = new VeloProperties();
        properties.getDateTimeFormat().setEnabled(true);
        properties.getDateTimeFormat().setDate("yyyy/MM/dd");

        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();

        new VeloWebMvcConfigurer(emptyConverterProvider(), properties).addFormatters(conversionService);

        assertThat(conversionService.convert("2024/03/31", LocalDate.class)).isEqualTo(LocalDate.of(2024, 3, 31));
    }

    @Test
    void shouldSkipDateFormattersWhenWebRegistrationDisabled() {
        VeloProperties properties = new VeloProperties();
        properties.getDateTimeFormat().setEnabled(true);
        properties.getDateTimeFormat().setDate("yyyy/MM/dd");
        properties.getWeb().setDateTimeFormatterRegistrationEnabled(false);

        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();

        new VeloWebMvcConfigurer(emptyConverterProvider(), properties).addFormatters(conversionService);

        assertThatThrownBy(() -> conversionService.convert("2024/03/31", LocalDate.class))
                .isInstanceOf(ConversionFailedException.class);
    }

    @Test
    void shouldApplyDatePatternToJavaUtilDateFormatter() {
        VeloProperties properties = new VeloProperties();
        properties.getDateTimeFormat().setEnabled(true);
        properties.getDateTimeFormat().setDate("yyyy/MM/dd");
        properties.getDateTimeFormat().setTimeZone("UTC");

        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        new VeloWebMvcConfigurer(emptyConverterProvider(), properties).addFormatters(conversionService);

        Date converted = conversionService.convert("2024/03/31", Date.class);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        assertThat(converted).isNotNull();
        assertThat(formatter.format(converted)).isEqualTo("2024/03/31");
    }
}
