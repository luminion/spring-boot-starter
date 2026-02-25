package io.github.luminion.starter.feature.web;

import io.github.luminion.starter.Prop;
import io.github.luminion.starter.feature.xss.XssCleaner;
import io.github.luminion.starter.feature.xss.converter.XssStringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.format.datetime.DateFormatterRegistrar;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * @author luminion
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class BaseWebMvcConfigurer implements WebMvcConfigurer {
    private final ApplicationContext applicationContext;
    private final Prop prop;

    public BaseWebMvcConfigurer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.prop = applicationContext.getBean(Prop.class);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {

        // 注册全局 XSS 格式化器（仅对 Web MVC 生效）
        // 注意：Formatter 接口不支持 ConditionalConverter 的 matches 逻辑（如 @XssIgnore）
        // 如果需要支持 @XssIgnore，建议改用 registry.addConverter(new
        // XssStringConverter(xssCleaner))
        ObjectProvider<XssCleaner> xssCleanerObjectProvider = applicationContext.getBeanProvider(XssCleaner.class);
        XssCleaner xssCleaner = xssCleanerObjectProvider.getIfAvailable();
        if (xssCleaner != null) {
            registry.addConverter(new XssStringConverter(xssCleaner));
        }

        // 注册日期时间格式化器 (从 Prop 配置中获取格式)
        String dateTimePattern = prop.getDateTimeFormat().getDateTime();
        String datePattern = prop.getDateTimeFormat().getDate();
        String timePattern = prop.getDateTimeFormat().getTime();
        String timeZone = prop.getDateTimeFormat().getTimeZone();

        // JSR-310：LocalDateTime / LocalDate / LocalTime
        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setTimeFormatter(DateTimeFormatter.ofPattern(timePattern));
        registrar.setDateFormatter(DateTimeFormatter.ofPattern(datePattern));
        registrar.setDateTimeFormatter(DateTimeFormatter.ofPattern(dateTimePattern));
        registrar.registerFormatters(registry);

        // java.util.Date / Calendar
        DateFormatterRegistrar dateRegistrar = new DateFormatterRegistrar();
        DateFormatter dateFormatter = new DateFormatter(dateTimePattern);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(timeZone));
        dateRegistrar.setFormatter(dateFormatter);
        dateRegistrar.registerFormatters(registry);

    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (prop.getWeb().isAllowCors()) {
            // 允许跨域
            registry.addMapping("/**")
                    .allowedOriginPatterns("*")
                    .allowCredentials(true)
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .maxAge(3600);
        }
    }

}
