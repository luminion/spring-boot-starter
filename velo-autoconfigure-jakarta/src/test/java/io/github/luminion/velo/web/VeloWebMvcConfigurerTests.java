package io.github.luminion.velo.web;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.xss.converter.XssStringConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.support.DefaultFormattingConversionService;

import java.lang.reflect.Field;
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
        properties.getSpringConverter().setDateTimeEnabled(true);
        properties.getDateTimeFormat().setDate("yyyy|MM|dd");

        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();

        new VeloWebMvcConfigurer(emptyConverterProvider(), properties).addFormatters(conversionService);

        assertThat(conversionService.convert("2024|03|31", LocalDate.class)).isEqualTo(LocalDate.of(2024, 3, 31));
    }

    @Test
    void shouldSkipDateFormattersWhenDateTimeFormatDisabled() {
        VeloProperties properties = new VeloProperties();
        properties.getSpringConverter().setDateTimeEnabled(false);
        properties.getDateTimeFormat().setDate("yyyy|MM|dd");

        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();

        new VeloWebMvcConfigurer(emptyConverterProvider(), properties).addFormatters(conversionService);

        assertThatThrownBy(() -> conversionService.convert("2024|03|31", LocalDate.class))
                .isInstanceOf(ConversionFailedException.class);
    }

    @Test
    void shouldApplyDateTimePatternToJavaUtilDateFormatter() {
        VeloProperties properties = new VeloProperties();
        properties.getSpringConverter().setDateTimeEnabled(true);
        properties.getDateTimeFormat().setDateTime("yyyy|MM|dd HH^mm^ss");
        properties.getDateTimeFormat().setDate("yyyy|MM|dd");
        properties.getDateTimeFormat().setTimeZone("UTC");

        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        new VeloWebMvcConfigurer(emptyConverterProvider(), properties).addFormatters(conversionService);

        Date converted = conversionService.convert("2024|03|31 08^09^10", Date.class);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy|MM|dd HH^mm^ss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        assertThat(converted).isNotNull();
        assertThat(formatter.format(converted)).isEqualTo("2024|03|31 08^09^10");

        Date dateOnly = conversionService.convert("2024|03|31", Date.class);
        assertThat(dateOnly).isNotNull();
        assertThat(formatter.format(dateOnly)).isEqualTo("2024|03|31 00^00^00");
    }

    @Test
    void shouldPreferDateTimeFormatAnnotationOverDefaultDateFormatter() throws NoSuchFieldException {
        VeloProperties properties = new VeloProperties();
        properties.getSpringConverter().setDateTimeEnabled(true);

        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        new VeloWebMvcConfigurer(emptyConverterProvider(), properties).addFormatters(conversionService);

        Field field = AnnotatedDateTarget.class.getDeclaredField("date");
        TypeDescriptor targetType = new TypeDescriptor(field);
        Date converted = (Date) conversionService.convert(
                "2024/03/31", TypeDescriptor.valueOf(String.class), targetType);

        assertThat(converted).isNotNull();
        assertThatThrownBy(() -> conversionService.convert(
                "2024-03-31", TypeDescriptor.valueOf(String.class), targetType))
                .isInstanceOf(ConversionFailedException.class);
    }

    static class AnnotatedDateTarget {
        @DateTimeFormat(pattern = "yyyy/MM/dd")
        private Date date;
    }
}
