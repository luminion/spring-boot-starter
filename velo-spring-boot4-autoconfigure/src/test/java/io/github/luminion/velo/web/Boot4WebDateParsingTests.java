package io.github.luminion.velo.web;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.xss.converter.XssStringConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.format.support.DefaultFormattingConversionService;

import java.time.DateTimeException;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Boot4WebDateParsingTests {
    @Test
    void shouldApplyShanghaiDefaultAndAcceptZoneIdOffsetsOnSpring7() {
        VeloProperties defaults = new VeloProperties();
        assertThat(defaults.getDateTimeFormat().getTimeZone()).isEqualTo("Asia/Shanghai");
        assertThat(configure(defaults).convert("1970-01-01 08:00:00", Date.class)).isEqualTo(new Date(0));
        for (String zone : new String[]{"+08:00", "GMT+08:00", "Asia/Shanghai"}) {
            VeloProperties properties = new VeloProperties();
            properties.getDateTimeFormat().setTimeZone(zone);
            assertThat(configure(properties).convert("1970-01-01 08:00:00", Date.class)).isEqualTo(new Date(0));
        }
    }

    @Test
    void shouldRejectInvalidTimeZonesOnSpring7() {
        for (String zone : new String[]{"Wrong/Zone", "+25:00", ""}) {
            VeloProperties properties = new VeloProperties();
            properties.getDateTimeFormat().setTimeZone(zone);
            assertThatThrownBy(() -> configure(properties)).isInstanceOf(DateTimeException.class);
        }
    }

    private DefaultFormattingConversionService configure(VeloProperties properties) {
        DefaultFormattingConversionService service = new DefaultFormattingConversionService();
        new VeloWebMvcConfigurer(new StaticListableBeanFactory().getBeanProvider(XssStringConverter.class), properties)
                .addFormatters(service);
        return service;
    }
}
