package io.github.luminion.velo.web;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.xss.converter.XssStringConverter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Arrays;
import java.util.TimeZone;

/**
 * Web MVC 增强配置。
 */
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class VeloWebMvcConfigurer implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(VeloWebMvcConfigurer.class);

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
        VeloProperties.WebProperties web = properties.getWeb();
        VeloProperties.CorsProperties cors = web.getCors();
        if (web.isAllowCors() || cors.isEnabled()) {
            warnIfCredentialedWildcard(cors);
            registry.addMapping("/**")
                    .allowedOriginPatterns(cors.getAllowedOriginPatterns())
                    .allowCredentials(cors.isAllowCredentials())
                    .allowedMethods(cors.getAllowedMethods())
                    .maxAge(cors.getMaxAge());
        }
    }

    // 允许携带凭证 + 未限定具体 origin(通配或为空)时，任意站点都能带 Cookie 跨域调用，属高危配置。
    // 保留默认值不做破坏性变更，仅在启动时告警，提示显式配置非通配的 allowed-origin-patterns。
    private void warnIfCredentialedWildcard(VeloProperties.CorsProperties cors) {
        if (!cors.isAllowCredentials()) {
            return;
        }
        String[] patterns = cors.getAllowedOriginPatterns();
        boolean unrestricted = patterns == null || patterns.length == 0
                || Arrays.stream(patterns).anyMatch(pattern -> "*".equals(pattern));
        if (unrestricted) {
            log.warn("[Velo Starter] CORS is enabled with allow-credentials=true but allowed-origin-patterns is " +
                    "wildcard or empty ({}). This lets any site send credentialed cross-origin requests. " +
                    "Configure velo.web.cors.allowed-origin-patterns with explicit origins, or set " +
                    "velo.web.cors.allow-credentials=false.", Arrays.toString(patterns));
        }
    }
}
