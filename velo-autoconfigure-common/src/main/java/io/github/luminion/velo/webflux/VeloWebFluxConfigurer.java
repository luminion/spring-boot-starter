package io.github.luminion.velo.webflux;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.xss.converter.XssStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.format.datetime.DateFormatterRegistrar;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.TimeZone;

/**
 * WebFlux 增强配置。
 *
 * <p>提供与 MVC 版一致的日期转换、XSS 字符串转换和 CORS 配置。配置方式复用
 * 现有的全局属性，不新增第二套响应式配置。</p>
 *
 * @author luminion
 * @since 1.3.1
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class VeloWebFluxConfigurer implements WebFluxConfigurer {

    private static final Logger log = LoggerFactory.getLogger(VeloWebFluxConfigurer.class);

    private final ObjectProvider<XssStringConverter> xssStringConverterProvider;

    private final VeloProperties properties;

    public VeloWebFluxConfigurer(ObjectProvider<XssStringConverter> xssStringConverterProvider,
            VeloProperties properties) {
        this.xssStringConverterProvider = xssStringConverterProvider;
        this.properties = properties;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        if (properties.getXss().isWebEnabled()) {
            XssStringConverter converter = xssStringConverterProvider.getIfAvailable();
            if (converter != null) {
                registry.addConverter(converter);
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
            DateFormatter dateFormatter = new DateFormatter(dateTimePattern);
            dateFormatter.setFallbackPatterns(datePattern);
            dateFormatter.setTimeZone(TimeZone.getTimeZone(timeZone));
            dateRegistrar.setFormatter(dateFormatter);
            dateRegistrar.registerFormatters(registry);
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        VeloProperties.CorsProperties cors = properties.getWeb().getCors();
        if (!cors.isEnabled()) {
            return;
        }
        warnIfCredentialedWildcard(cors);
        registry.addMapping("/**")
                .allowedOriginPatterns(cors.getAllowedOriginPatterns())
                .allowCredentials(cors.isAllowCredentials())
                .allowedMethods(cors.getAllowedMethods())
                .maxAge(cors.getMaxAge());
    }

    private void warnIfCredentialedWildcard(VeloProperties.CorsProperties cors) {
        if (!cors.isAllowCredentials()) {
            return;
        }
        String[] patterns = cors.getAllowedOriginPatterns();
        boolean unrestricted = patterns == null || patterns.length == 0
                || Arrays.stream(patterns).anyMatch(pattern -> "*".equals(pattern));
        if (unrestricted) {
            log.warn(
                    "[Velo Starter] CORS is enabled with allow-credentials=true but allowed-origin-patterns is "
                            + "wildcard or empty ({}). This lets any site send credentialed cross-origin requests. "
                            + "Configure velo.web.cors.allowed-origin-patterns with explicit origins, or set "
                            + "velo.web.cors.allow-credentials=false.", Arrays.toString(patterns));
        }
    }
}
