package io.github.luminion.velo.converter;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.converter.datetime.StringToJavaUtilDateConverter;
import io.github.luminion.velo.converter.datetime.StringToLocalDateConverter;
import io.github.luminion.velo.converter.datetime.StringToLocalDateTimeConverter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class VeloDateTimeFormatAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    VeloCoreAutoConfiguration.class,
                    VeloDateTimeFormatAutoConfiguration.class
            ));

    @Test
    void shouldRejectInvalidCalendarDatesLikeSpringFormatters() {
        contextRunner.run(context -> {
            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                    context.getBean(StringToLocalDateConverter.class).convert("2026-02-30"))
                    .isInstanceOf(java.time.format.DateTimeParseException.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                    context.getBean(StringToLocalDateTimeConverter.class).convert("2026-02-30 12:00:00"))
                    .isInstanceOf(java.time.format.DateTimeParseException.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                    context.getBean(StringToJavaUtilDateConverter.class).convert("2026-02-30"))
                    .isInstanceOf(java.time.format.DateTimeParseException.class);
            assertThat(context.getBean(StringToLocalDateConverter.class).convert("2024-02-29"))
                    .isEqualTo(LocalDate.of(2024, 2, 29));
        });
    }

    @Test
    void shouldRegisterDateTimeConvertersByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(StringToLocalDateConverter.class);
            assertThat(context).hasSingleBean(StringToLocalDateTimeConverter.class);
        });
    }

    @Test
    void shouldConvertJavaUtilDateWithDateTimeAndDatePatternsByDefault() {
        contextRunner.run(context -> {
            StringToJavaUtilDateConverter converter = context.getBean(StringToJavaUtilDateConverter.class);
            ZoneId zoneId = ZoneId.of("GMT+8");

            assertThat(converter.convert("2024-01-02 03:04:05"))
                    .isEqualTo(Date.from(LocalDateTime.of(2024, 1, 2, 3, 4, 5).atZone(zoneId).toInstant()));
            assertThat(converter.convert("2024-01-02"))
                    .isEqualTo(Date.from(LocalDate.of(2024, 1, 2).atStartOfDay(zoneId).toInstant()));
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

    @Test
    void shouldFailApplicationStartupForInvalidDateTimePattern() {
        contextRunner
                .withPropertyValues("velo.date-time-format.date=invalid[")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldFailApplicationStartupForInvalidTimeZone() {
        contextRunner
                .withPropertyValues("velo.date-time-format.time-zone=invalid-zone")
                .run(context -> assertThat(context).hasFailed());
    }
}
