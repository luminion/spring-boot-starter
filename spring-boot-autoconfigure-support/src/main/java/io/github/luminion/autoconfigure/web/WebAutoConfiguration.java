package io.github.luminion.autoconfigure.web;

import io.github.luminion.autoconfigure.core.properties.DateTimeFormatProperties;
import io.github.luminion.autoconfigure.web.converter.support.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
@EnableConfigurationProperties(DateTimeFormatProperties.class)
@ConditionalOnProperty(value = "luminion.web.enabled", havingValue = "true", matchIfMissing = true)
public class WebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public StringToDateConverter string2DateConverter(DateTimeFormatProperties properties) {
        log.debug("StringToDateConverter Configured");
        return new StringToDateConverter(properties.getDateTime(), properties.getTimeZone());
    }

    @Bean
    @ConditionalOnMissingBean
    public StringToLocalDateTimeConverter string2LocalDateTimeConverter(DateTimeFormatProperties properties) {
        log.debug("StringToLocalDateTimeConverter Configured");
        return new StringToLocalDateTimeConverter(properties.getDateTime());
    }

    @Bean
    @ConditionalOnMissingBean
    public StringToLocalDateConverter string2LocalDateConverter(DateTimeFormatProperties properties) {
        log.debug("StringToLocalDateConverter configured");
        return new StringToLocalDateConverter(properties.getDate());
    }

    @Bean
    @ConditionalOnMissingBean
    public StringToLocalTimeConverter string2LocalTimeConverter(DateTimeFormatProperties properties) {
        log.debug("StringToLocalTimeConverter Configured");
        return new StringToLocalTimeConverter(properties.getTime());
    }

    @Bean
    @ConditionalOnMissingBean
    public StringToSqlDateConverter string2SqlDateConverter(DateTimeFormatProperties properties) {
        log.debug("StringToSqlDateConverter Configured");
        return new StringToSqlDateConverter(properties.getDate());
    }
    
    @Bean
    @ConditionalOnMissingBean
    public StringToSqlTimeConverter string2SqlTimeConverter(DateTimeFormatProperties properties) {
        log.debug("StringToSqlTimeConverter Configured");
        return new StringToSqlTimeConverter(properties.getTime());
    }

    @Bean
    @ConditionalOnMissingBean
    public StringToSqlTimestampConverter string2SqlTimestampConverter(DateTimeFormatProperties properties) {
        log.debug("StringToSqlTimestampConverter Configured");
        return new StringToSqlTimestampConverter(properties.getDateTime());
    }

}
