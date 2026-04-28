package io.github.luminion.velo.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.spi.EnumFieldConvention;
import io.github.luminion.velo.spi.JsonProcessorProvider;
import io.github.luminion.velo.jackson.deserializer.JacksonStringDeserializer;
import io.github.luminion.velo.jackson.serializer.JacksonStringSerializer;
import io.github.luminion.velo.jackson.serializer.JsonEnumSerializerModifier;
import io.github.luminion.velo.jackson.serializer.UnsafeBigIntegerToStringSerializer;
import io.github.luminion.velo.jackson.serializer.UnsafeLongToStringSerializer;
import io.github.luminion.velo.xss.XssCleaner;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
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
 * Jackson 配置。
 *
 * @see org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
 */
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
@ConditionalOnProperty(prefix = "velo.jackson", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloJacksonAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({Jackson2ObjectMapperBuilder.class})
    static class Jackson2ObjectMapperBuilderCustomizerConfiguration {

        @Bean
        @Order(-1)
        public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer(VeloProperties properties,
                                                                                           BeanFactory beanFactory) {
            return builder -> {
                String dateTimeFormat = properties.getDateTimeFormat().getDateTime();
                String dateFormat = properties.getDateTimeFormat().getDate();
                String timeFormat = properties.getDateTimeFormat().getTime();
                String timeZoneId = properties.getDateTimeFormat().getTimeZone();
                TimeZone timeZone = TimeZone.getTimeZone(timeZoneId);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateTimeFormat);
                simpleDateFormat.setTimeZone(timeZone);
                VeloProperties.JacksonProperties jacksonProperties = properties.getJackson();
                VeloProperties.JacksonProperties.DateTimeProperties jacksonDateTime = jacksonProperties.getDateTime();

                // 先收口基础时间与容错策略，再让业务自定义器继续叠加更细粒度的能力。
                builder
                        .failOnEmptyBeans(false)
                        .failOnUnknownProperties(false);

                if (jacksonDateTime.isEnabled()) {
                    builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                    builder.dateFormat(simpleDateFormat)
                            .timeZone(timeZone);
                }

                if (jacksonProperties.isWriteUnsafeIntegerAsString()) {
                    UnsafeLongToStringSerializer longSerializer = new UnsafeLongToStringSerializer();
                    builder.serializerByType(Long.class, longSerializer)
                            .serializerByType(Long.TYPE, longSerializer)
                            .serializerByType(BigInteger.class, new UnsafeBigIntegerToStringSerializer());
                }
                if (jacksonProperties.isWriteBigDecimalAsString()) {
                    builder.serializerByType(BigDecimal.class, ToStringSerializer.instance);
                }
                if (jacksonProperties.isWriteFloatingPointAsString()) {
                    builder.serializerByType(Double.class, ToStringSerializer.instance)
                            .serializerByType(Double.TYPE, ToStringSerializer.instance)
                            .serializerByType(Float.class, ToStringSerializer.instance)
                            .serializerByType(Float.TYPE, ToStringSerializer.instance);
                }

                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat);
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(dateFormat);
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(timeFormat);

                if (jacksonDateTime.isEnabled()) {
                    builder.deserializerByType(LocalDateTime.class,
                                    new LocalDateTimeDeserializer(dateTimeFormatter))
                            .deserializerByType(LocalDate.class,
                                    new LocalDateDeserializer(dateFormatter))
                            .deserializerByType(LocalTime.class,
                                    new LocalTimeDeserializer(timeFormatter));
                }

                if (jacksonDateTime.isEnabled()) {
                    builder.serializers(new LocalDateTimeSerializer(dateTimeFormatter))
                            .serializers(new LocalDateSerializer(dateFormatter))
                            .serializers(new LocalTimeSerializer(timeFormatter));
                }

                if (jacksonProperties.getStringConverters().isEnabled()) {
                    ObjectProvider<JsonProcessorProvider> jsonProcessorProviderObjectProvider = beanFactory
                            .getBeanProvider(JsonProcessorProvider.class);
                    jsonProcessorProviderObjectProvider.ifAvailable(bean -> {
                        ObjectProvider<XssCleaner> xssCleanerObjectProvider = beanFactory
                                .getBeanProvider(XssCleaner.class);
                        XssCleaner xssCleaner = xssCleanerObjectProvider.getIfAvailable();

                        builder.deserializerByType(String.class,
                                new JacksonStringDeserializer(bean, xssCleaner));
                        builder.serializerByType(String.class, new JacksonStringSerializer(bean));
                    });
                }

                if (jacksonProperties.isEnumDescriptionEnabled()) {
                    ObjectProvider<EnumFieldConvention> enumFieldConventionObjectProvider = beanFactory
                            .getBeanProvider(EnumFieldConvention.class);
                    enumFieldConventionObjectProvider.ifAvailable(bean -> {
                        SimpleModule enumModule = new SimpleModule();
                        enumModule.setSerializerModifier(new JsonEnumSerializerModifier(bean));
                        builder.modules(enumModule);
                    });
                }

            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({RedisTemplate.class, Jackson2ObjectMapperBuilder.class})
    static class JacksonRedisConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "redisSerializer")
        public RedisSerializer<Object> redisSerializer() {
            // Redis 序列化需要一份稳定的 ObjectMapper，避免被 Web 层 builder 上的自定义器意外改写。
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            objectMapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            objectMapper.registerModule(new JavaTimeModule());

            // 写入类型信息后，Redis 中的多态对象在反序列化时才能还原真实类型。
            objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                    ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
            GenericJackson2JsonRedisSerializer.registerNullValueSerializer(objectMapper, null);
            return new GenericJackson2JsonRedisSerializer(objectMapper);
        }
    }
}
