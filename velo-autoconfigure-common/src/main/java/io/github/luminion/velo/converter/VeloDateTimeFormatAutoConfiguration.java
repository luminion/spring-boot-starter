package io.github.luminion.velo.converter;

import io.github.luminion.velo.converter.datetime.StringToJavaUtilDateConverter;
import io.github.luminion.velo.converter.datetime.StringToLocalDateConverter;
import io.github.luminion.velo.converter.datetime.StringToLocalDateTimeConverter;
import io.github.luminion.velo.converter.datetime.StringToLocalTimeConverter;
import io.github.luminion.velo.VeloProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 日期时间转换自动配置实现。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "velo.date-time-format", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloDateTimeFormatAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "velo.date-time-format.converters", name = "enabled", havingValue = "true")
    static class DateTimeConverterConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "velo.date-time-format.converters", name = "java-util-date-converter-enabled", havingValue = "true", matchIfMissing = true)
        public StringToJavaUtilDateConverter stringToDateConverter(VeloProperties properties) {
            return new StringToJavaUtilDateConverter(properties.getDateTimeFormat().getDateTime(),
                    properties.getDateTimeFormat().getTimeZone());
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "velo.date-time-format.converters", name = "local-date-time-converter-enabled", havingValue = "true", matchIfMissing = true)
        public StringToLocalDateTimeConverter stringToLocalDateTimeConverter(VeloProperties properties) {
            return new StringToLocalDateTimeConverter(properties.getDateTimeFormat().getDateTime());
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "velo.date-time-format.converters", name = "local-date-converter-enabled", havingValue = "true", matchIfMissing = true)
        public StringToLocalDateConverter stringToLocalDateConverter(VeloProperties properties) {
            return new StringToLocalDateConverter(properties.getDateTimeFormat().getDate());
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "velo.date-time-format.converters", name = "local-time-converter-enabled", havingValue = "true", matchIfMissing = true)
        public StringToLocalTimeConverter stringToLocalTimeConverter(VeloProperties properties) {
            return new StringToLocalTimeConverter(properties.getDateTimeFormat().getTime());
        }
    }
}
