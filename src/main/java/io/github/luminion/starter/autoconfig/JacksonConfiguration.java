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
import io.github.luminion.starter.Prop;
import io.github.luminion.starter.xss.XssCleaner;
import io.github.luminion.starter.jackson.deserializer.JacksonStringDeserializer;
import io.github.luminion.starter.jackson.serializer.JacksonStringSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.ApplicationContext;
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
public class JacksonConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ Jackson2ObjectMapperBuilder.class })
    static class Jackson2ObjectMapperBuilderCustomizerConfiguration {

        /**
         * jackson2对象映射器生成器定制器
         *
         * @param prop               配置属性
         * @param xssCleanerProvider XSS清理器提供者
         * @param applicationContext Spring 上下文
         * @return jackson对象映射器生成器定制器
         */
        @Bean
        @Order(-1)
        public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer(Prop prop, ObjectProvider<XssCleaner> xssCleanerProvider,
                ApplicationContext applicationContext) {
            log.debug("Jackson2ObjectMapperBuilderCustomizer Configured");
            return builder -> {
                String dateTimeFormat = prop.getDateTimeFormat().getDateTime();
                String dateFormat = prop.getDateTimeFormat().getDate();
                String timeFormat = prop.getDateTimeFormat().getTime();
                String timeZoneId = prop.getDateTimeFormat().getTimeZone();
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
                        .timeZone(timeZone);

                // 数字精度丢失处理
                builder.serializerByType(Long.class, ToStringSerializer.instance)
                        .serializerByType(Long.TYPE, ToStringSerializer.instance)
                        .serializerByType(Double.class, ToStringSerializer.instance)
                        .serializerByType(Double.TYPE, ToStringSerializer.instance)
                        .serializerByType(BigInteger.class, ToStringSerializer.instance)
                        .serializerByType(BigDecimal.class, ToStringSerializer.instance);

                // Java8 时间解析处理
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat);
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(dateFormat);
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(timeFormat);

                builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter))
                        .deserializerByType(LocalDate.class, new LocalDateDeserializer(dateFormatter))
                        .deserializerByType(LocalTime.class, new LocalTimeDeserializer(timeFormatter));

                builder.serializers(new LocalDateTimeSerializer(dateTimeFormatter))
                        .serializers(new LocalDateSerializer(dateFormatter))
                        .serializers(new LocalTimeSerializer(timeFormatter));

                XssCleaner xssCleaner = xssCleanerProvider.getIfAvailable();
                builder.deserializerByType(String.class, new JacksonStringDeserializer(xssCleaner, applicationContext));

                // 统一字符串处理（Mask）
                builder.serializerByType(String.class,new JacksonStringSerializer(applicationContext));
            };
        }
    }

    /**
     * jackson redis配置
     *
     * @author luminion
     * @since 1.0.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ RedisTemplate.class, Jackson2ObjectMapperBuilder.class })
    static class JacksonRedisConfiguration {

        /**
         * redis序列化程序
         *
         * @return redis序列化程序 <object>
         */
        @Bean
        @ConditionalOnMissingBean(name = "redisSerializer")
        public RedisSerializer<Object> redisSerializer() {
            // public RedisSerializer<Object> redisSerializer(Jackson2ObjectMapperBuilder
            // jackson2ObjectMapperBuilder) {
            // ObjectMapper objectMapper = jackson2ObjectMapperBuilder.build();
            // 不使用jackson2ObjectMapperBuilder.build(), 该配置器会覆盖自定义的ObjectMapper配置, 比如数据脱敏之类的
            ObjectMapper objectMapper = new ObjectMapper();
            // 反序列化时候遇到不匹配的属性并不抛出异常
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            // 序列化时候遇到空对象不抛出异常
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            // 反序列化的时候如果是无效子类型,不抛出异常
            objectMapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
            // 不使用默认的dateTime进行序列化,
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            // 使用JSR310提供的序列化类,里面包含了大量的JDK8时间序列化类
            objectMapper.registerModule(new JavaTimeModule());
            // 启用反序列化所需的类型信息,在属性中添加@class
            objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                    ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
            // 配置null值的序列化器
            GenericJackson2JsonRedisSerializer.registerNullValueSerializer(objectMapper, null);
            return new GenericJackson2JsonRedisSerializer(objectMapper);
        }
    }

}
