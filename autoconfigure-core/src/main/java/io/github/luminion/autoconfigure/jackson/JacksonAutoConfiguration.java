package io.github.luminion.autoconfigure.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import io.github.luminion.autoconfigure.DateTimeFormatProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * jackson配置
 *
 * @author luminion
 * @see org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
@ConditionalOnProperty(value = "turbo.jackson.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({JacksonProperties.class, DateTimeFormatProperties.class})
public class JacksonAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Jackson2ObjectMapperBuilder.class)
    static class Jackson2ObjectMapperBuilderCustomizerConfiguration {

        @Bean
        @Order(-1)// 使Jackson2ObjectMapperBuilder在获取Jackson2ObjectMapperBuilderCustomizer时, 获取该配置先于StandardJackson2ObjectMapperBuilderCustomizer
        public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer(DateTimeFormatProperties dateTimeFormatProperties, JacksonProperties jacksonProperties) {
            log.debug("Jackson2ObjectMapperBuilderCustomizer Configured");
            return builder -> {
                String dateTimeFormat = dateTimeFormatProperties.getDateTime();
                String dateFormat = dateTimeFormatProperties.getDate();
                String timeFormat = dateTimeFormatProperties.getTime();
                String timeZoneId = dateTimeFormatProperties.getTimeZone();
//                ZoneId zoneId = ZoneId.of(timeZoneId);
                TimeZone timeZone = TimeZone.getTimeZone(timeZoneId);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateTimeFormat);
                simpleDateFormat.setTimeZone(timeZone);
                builder
                        // 序列化时，对象为 null，是否抛异常
                        .failOnEmptyBeans(false)
                        // 反序列化时，json 中包含 pojo 不存在属性时，是否抛异常
                        .failOnUnknownProperties(false)
                        // 禁止将 java.util.Date、Calendar 序列化为数字(时间戳)
                        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        // 设置 java.util.Date, Calendar 序列化、反序列化的格式
                        .dateFormat(simpleDateFormat)
                        // 设置 java.util.Date, Calendar 序列化、反序列化的时区
                        .timeZone(timeZone)
//                    // null 不参与序列化
//                    .serializationInclusion(JsonInclude.Include.NON_NULL)
                ;
                /*
                Jackson 序列化, 解决后端返回的数字类型在前端精度丢失的问题
                JS 遵循 IEEE 754 规范, 能精准表示的最大整数是 Math.pow(2, 53)，十进制即 9007199254740992，任何大于9007199254740992都会出现精度丢失的问题
                也可使用全局配置项
                spring:
                  jackson:
                    generator:
                      write_numbers_as_strings: true #序列化的时候，将数值类型全部转换成字符串返回
                 */
                if (jacksonProperties.isLongToString()){
                    builder.serializerByType(Long.class, ToStringSerializer.instance);
                    builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
                }
                if (jacksonProperties.isDoubleToString()){
                    builder.serializerByType(Double.class, ToStringSerializer.instance);
                    builder.serializerByType(Double.TYPE, ToStringSerializer.instance);
                }
                if (jacksonProperties.isBigIntegerToString()){
                    builder.serializerByType(BigInteger.class, ToStringSerializer.instance);
                }
                if (jacksonProperties.isBigDecimalToString()){
                    builder.serializerByType(BigDecimal.class, ToStringSerializer.instance);
                }

                // 序列化器
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat);
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(dateFormat);
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(timeFormat);

                // 配置 Jackson 反序列化 LocalDateTime、LocalDate、LocalTime 时使用的格式
                builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
                builder.deserializerByType(LocalDate.class, new LocalDateDeserializer(dateFormatter));
                builder.deserializerByType(LocalTime.class, new LocalTimeDeserializer(timeFormatter));

                // 配置 Jackson 序列化 LocalDateTime、LocalDate、LocalTime 时使用的格式
                builder.serializers(new LocalDateTimeSerializer(dateTimeFormatter));
                builder.serializers(new LocalDateSerializer(dateFormatter));
                builder.serializers(new LocalTimeSerializer(timeFormatter));
            };
        }
    }

}
