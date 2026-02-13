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
    public StringToDateConverter stringToDateConverter(Prop properties) {
        return new StringToDateConverter(properties.getDateTimeFormat().getDateTime(), properties.getDateTimeFormat().getTimeZone());
    }

    @Bean
    @ConditionalOnMissingBean
    public StringToLocalDateTimeConverter stringToLocalDateTimeConverter(Prop properties) {
        return new StringToLocalDateTimeConverter(properties.getDateTimeFormat().getDateTime());
    }

    @Bean
    @ConditionalOnMissingBean
    public StringToLocalDateConverter stringToLocalDateConverter(Prop properties) {
        return new StringToLocalDateConverter(properties.getDateTimeFormat().getDate());
    }

    @Bean
    @ConditionalOnMissingBean
    public StringToLocalTimeConverter stringToLocalTimeConverter(Prop properties) {
        return new StringToLocalTimeConverter(properties.getDateTimeFormat().getTime());
    }

    @Bean
    @ConditionalOnMissingBean
    public StringToSqlDateConverter stringToSqlDateConverter(Prop properties) {
        return new StringToSqlDateConverter(properties.getDateTimeFormat().getDate());
    }
    
    @Bean
    @ConditionalOnMissingBean
    public StringToSqlTimeConverter stringToSqlTimeConverter(Prop properties) {
        return new StringToSqlTimeConverter(properties.getDateTimeFormat().getTime());
    }

    @Bean
    @ConditionalOnMissingBean
    public StringToSqlTimestampConverter stringToSqlTimestampConverter(Prop properties) {
        return new StringToSqlTimestampConverter(properties.getDateTimeFormat().getDateTime());
    }

}
