package io.github.luminion.autoconfigure.converter;

import io.github.luminion.autoconfigure.DateTimeFormatProperties;
import io.github.luminion.autoconfigure.converter.support.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 转换器配置
 *
 * @author luminion
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(value = "turbo.converter.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({ConverterProperties.class, DateTimeFormatProperties.class})
public class ConverterAutoConfiguration {

    @Bean
    @ConditionalOnProperty(value = "turbo.converter.string-to-date", havingValue = "true", matchIfMissing = true)
    public String2DateConverter string2DateConverter(DateTimeFormatProperties properties) {
        log.debug("String2DateConverter Configured");
        return new String2DateConverter(properties.getDateTime(), properties.getTimeZone());
    }

    @Bean
    @ConditionalOnProperty(value = "turbo.converter.string-to-local-date-time", havingValue = "true", matchIfMissing = true)
    public String2LocalDateTimeConverter string2LocalDateTimeConverter(DateTimeFormatProperties properties) {
        log.debug("String2LocalDateTimeConverter Configured");
        return new String2LocalDateTimeConverter(properties.getDateTime());
    }

    @Bean
    @ConditionalOnProperty(value = "turbo.converter.string-to-local-date", havingValue = "true", matchIfMissing = true)
    public String2LocalDateConverter string2LocalDateConverter(DateTimeFormatProperties properties) {
        log.debug("String2LocalDateConverter configured");
        return new String2LocalDateConverter(properties.getDate());
    }

    @Bean
    @ConditionalOnProperty(value = "turbo.converter.string-to-local-time", havingValue = "true", matchIfMissing = true)
    public String2LocalTimeConverter string2LocalTimeConverter(DateTimeFormatProperties properties) {
        log.debug("String2LocalTimeConverter Configured");
        return new String2LocalTimeConverter(properties.getTime());
    }

    @Bean
    @ConditionalOnProperty(value = "turbo.converter.string-to-sql-date", havingValue = "true", matchIfMissing = true)
    public String2SqlDateConverter string2SqlDateConverter(DateTimeFormatProperties properties) {
        log.debug("String2SqlDateConverter Configured");
        return new String2SqlDateConverter(properties.getDate());
    }
    
    @Bean
    @ConditionalOnProperty(value = "turbo.converter.string-to-sql-time", havingValue = "true", matchIfMissing = true)
    public String2SqlTimeConverter string2SqlTimeConverter(DateTimeFormatProperties properties) {
        log.debug("String2SqlTimeConverter Configured");
        return new String2SqlTimeConverter(properties.getTime());
    }

    @Bean
    @ConditionalOnProperty(value = "turbo.converter.string-to-sql-timestamp", havingValue = "true", matchIfMissing = true)
    public String2SqlTimestampConverter string2SqlTimestampConverter(DateTimeFormatProperties properties) {
        log.debug("String2SqlTimestampConverter Configured");
        return new String2SqlTimestampConverter(properties.getDateTime());
    }

}
