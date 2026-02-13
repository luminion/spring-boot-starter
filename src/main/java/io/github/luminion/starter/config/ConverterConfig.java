package io.github.luminion.starter.config;

import io.github.luminion.starter.Prop;
import io.github.luminion.starter.converter.XssCleanerConverter;
import io.github.luminion.starter.converter.support.*;
import io.github.luminion.starter.web.formatter.MaskAnnotationFormatterFactory;
import io.github.luminion.starter.web.formatter.UnmaskAnnotationFormatterFactory;
import io.github.luminion.starter.xss.XssCleaner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * 转换器配置
 *
 * @author luminion
 */
@Slf4j
//@AutoConfiguration
@Deprecated
public class ConverterConfig {

    @Bean
    @ConditionalOnMissingBean
    public StringToDateConverter stringToDateConverter(Prop properties) {
        return new StringToDateConverter(properties.getDateTimeFormat().getDateTime(),
                properties.getDateTimeFormat().getTimeZone());
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

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(XssCleaner.class)
    public XssCleanerConverter stringToHtmlConverter(XssCleaner xssCleaner) {
        return new XssCleanerConverter(xssCleaner);
    }

    @Bean
    @ConditionalOnMissingBean
    public MaskAnnotationFormatterFactory maskAnnotationFormatterFactory(ApplicationContext applicationContext) {
        return new MaskAnnotationFormatterFactory(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public UnmaskAnnotationFormatterFactory unmaskAnnotationFormatterFactory(ApplicationContext applicationContext) {
        return new UnmaskAnnotationFormatterFactory(applicationContext);
    }

}
