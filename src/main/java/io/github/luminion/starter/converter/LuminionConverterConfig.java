package io.github.luminion.starter.converter;

import io.github.luminion.starter.converter.datetime.*;
import io.github.luminion.starter.core.Prop;
import io.github.luminion.starter.feature.converter.datetime.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 转换器配置
 *
 * @deprecated 该类会全局修改spring中所有对应转化, 需确认兼容后使用(若无特殊处理, 标准程序一般可直接使用)
 * @author luminion
 */
@AutoConfiguration
@ConditionalOnBean(Prop.class)
@Deprecated
public class LuminionConverterConfig {

    @Bean
    @ConditionalOnMissingBean
    public StringToJavaUtilDateConverter stringToDateConverter(Prop properties) {
        return new StringToJavaUtilDateConverter(properties.getDateTimeFormat().getDateTime(),
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


//    @Bean// sql包极少使用
    @ConditionalOnMissingBean
    public StringToSqlDateConverter stringToSqlDateConverter(Prop properties) {
        return new StringToSqlDateConverter(properties.getDateTimeFormat().getDate());
    }

//    @Bean// sql包极少使用
    @ConditionalOnMissingBean
    public StringToSqlTimeConverter stringToSqlTimeConverter(Prop properties) {
        return new StringToSqlTimeConverter(properties.getDateTimeFormat().getTime());
    }

//    @Bean//sql包极少使用
    @ConditionalOnMissingBean
    public StringToSqlTimestampConverter stringToSqlTimestampConverter(Prop properties) {
        return new StringToSqlTimestampConverter(properties.getDateTimeFormat().getDateTime());
    }

}
