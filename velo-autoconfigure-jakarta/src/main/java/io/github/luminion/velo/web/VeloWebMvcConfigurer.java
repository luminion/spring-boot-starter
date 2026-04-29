package io.github.luminion.velo.web;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.xss.converter.XssStringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.format.datetime.DateFormatterRegistrar;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Web MVC 增强配置。
 */
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class VeloWebMvcConfigurer implements WebMvcConfigurer {

    private final ObjectProvider<XssStringConverter> xssStringConverterProvider;
    private final VeloProperties properties;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        VeloProperties.XssProperties xss = properties.getWeb().getXss();
        if (xss.isEnabled()) {
            XssStringConverter xssStringConverter = xssStringConverterProvider.getIfAvailable();
            if (xssStringConverter != null) {
                registry.addConverter(xssStringConverter);
            }
        }

        if (properties.getSpringConverter().isDateTimeEnabled()) {
            String dateTimePattern = properties.getDateTimeFormat().getDateTime();
            String datePattern = properties.getDateTimeFormat().getDate();
            String timePattern = properties.getDateTimeFormat().getTime();
            String timeZone = properties.getDateTimeFormat().getTimeZone();

            DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
            registrar.setTimeFormatter(DateTimeFormatter.ofPattern(timePattern));
            registrar.setDateFormatter(DateTimeFormatter.ofPattern(datePattern));
            registrar.setDateTimeFormatter(DateTimeFormatter.ofPattern(dateTimePattern));
            registrar.registerFormatters(registry);

            DateFormatterRegistrar dateRegistrar = new DateFormatterRegistrar();
            DateFormatter dateFormatter = new DateFormatter(datePattern);
            dateFormatter.setTimeZone(TimeZone.getTimeZone(timeZone));
            dateRegistrar.setFormatter(dateFormatter);
            dateRegistrar.registerFormatters(registry);
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (properties.getWeb().isAllowCors()) {
            registry.addMapping("/**")
                    .allowedOriginPatterns("*")
                    .allowCredentials(true)
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .maxAge(3600);
        }
    }
}
