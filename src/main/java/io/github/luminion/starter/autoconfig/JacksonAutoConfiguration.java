package io.github.luminion.starter.autoconfig;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import io.github.luminion.starter.core.properties.DateTimeFormatProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
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
@ConditionalOnProperty(value = "luminion.jackson.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({DateTimeFormatProperties.class})
public class JacksonAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({Jackson2ObjectMapperBuilder.class})
    static class Jackson2ObjectMapperBuilderCustomizerConfiguration {

        @Bean
        @Order(-1)
// 使Jackson2ObjectMapperBuilder在获取Jackson2ObjectMapperBuilderCustomizer时, 获取该配置先于StandardJackson2ObjectMapperBuilderCustomizer
        public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer(DateTimeFormatProperties dateTimeFormatProperties) {
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
                builder.serializerByType(Long.class, ToStringSerializer.instance);
                builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
                builder.serializerByType(Double.class, ToStringSerializer.instance);
                builder.serializerByType(Double.TYPE, ToStringSerializer.instance);
                builder.serializerByType(BigInteger.class, ToStringSerializer.instance);
                builder.serializerByType(BigDecimal.class, ToStringSerializer.instance);

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

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({RedisTemplate.class, Jackson2ObjectMapperBuilder.class})
    static class JacksonRedisConfiguration {
        @Bean
        @ConditionalOnMissingBean(name = "redisSerializer")
        public RedisSerializer<Object> redisSerializer(Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder) {
            ObjectMapper objectMapper = jackson2ObjectMapperBuilder.build();
            // 反序列化时候遇到不匹配的属性并不抛出异常
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            // 序列化时候遇到空对象不抛出异常
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            // 反序列化的时候如果是无效子类型,不抛出异常
            objectMapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
            // 不使用默认的dateTime进行序列化,
            objectMapper.configure(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS, false);
            // 使用JSR310提供的序列化类,里面包含了大量的JDK8时间序列化类
            objectMapper.registerModule(new JavaTimeModule());
            // 启用反序列化所需的类型信息,在属性中添加@class
            objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
            // 配置null值的序列化器
            GenericJackson2JsonRedisSerializer.registerNullValueSerializer(objectMapper, null);
            return new GenericJackson2JsonRedisSerializer(objectMapper);
        }
    }

}
