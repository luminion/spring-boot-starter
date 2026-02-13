package io.github.luminion.starter.autoconfig;

import io.github.luminion.starter.Prop;
import io.github.luminion.starter.converter.support.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 转换器配置
 *
 * @author luminion
 */
@Slf4j
@AutoConfiguration
public class ConverterConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public StringToDateConverter string2DateConverter(Prop properties) {
        log.debug("StringToDateConverter Configured");
        return new StringToDateConverter(properties.getDateTimeFormat().getDateTime(), properties.getDateTimeFormat().getTimeZone());
    }

    @Bean
    @ConditionalOnMissingBean
    public StringToLocalDateTimeConverter string2LocalDateTimeConverter(Prop properties) {
        log.debug("StringToLocalDateTimeConverter Configured");
        return new StringToLocalDateTimeConverter(properties.getDateTimeFormat().getDateTime());
    }

    @Bean
    @ConditionalOnMissingBean
    public StringToLocalDateConverter string2LocalDateConverter(Prop properties) {
        log.debug("StringToLocalDateConverter configured");
        return new StringToLocalDateConverter(properties.getDateTimeFormat().getDate());
    }

    @Bean
    @ConditionalOnMissingBean
    public StringToLocalTimeConverter string2LocalTimeConverter(Prop properties) {
        log.debug("StringToLocalTimeConverter Configured");
        return new StringToLocalTimeConverter(properties.getDateTimeFormat().getTime());
    }

    @Bean
    @ConditionalOnMissingBean
    public StringToSqlDateConverter string2SqlDateConverter(Prop properties) {
        log.debug("StringToSqlDateConverter Configured");
        return new StringToSqlDateConverter(properties.getDateTimeFormat().getDate());
    }
    
    @Bean
    @ConditionalOnMissingBean
    public StringToSqlTimeConverter string2SqlTimeConverter(Prop properties) {
        log.debug("StringToSqlTimeConverter Configured");
        return new StringToSqlTimeConverter(properties.getDateTimeFormat().getTime());
    }

    @Bean
    @ConditionalOnMissingBean
    public StringToSqlTimestampConverter string2SqlTimestampConverter(Prop properties) {
        log.debug("StringToSqlTimestampConverter Configured");
        return new StringToSqlTimestampConverter(properties.getDateTimeFormat().getDateTime());
    }

}
