package io.github.luminion.velo.jackson;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.converter.datetime.FlexibleDateFormat;
import io.github.luminion.velo.spi.JsonProcessorProvider;
import io.github.luminion.velo.jackson.deserializer.JacksonStringDeserializer;
import io.github.luminion.velo.jackson.serializer.ConfigurableBigDecimalSerializer;
import io.github.luminion.velo.jackson.serializer.JacksonStringSerializer;
import io.github.luminion.velo.jackson.serializer.JsonEnumSerializerModifier;
import io.github.luminion.velo.jackson.serializer.NumericArraySerializer;
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
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.format.datetime.standard.DateTimeFormatterFactory;
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
        public com.fasterxml.jackson.databind.Module veloEnumModule(VeloProperties properties) {
            SimpleModule module = new SimpleModule("velo-enum");
            if (properties.getJackson().isEnumDescEnabled()) {
                module.setSerializerModifier(new JsonEnumSerializerModifier(properties.getJackson()));
            }
            return module;
        }

        @Bean
        @Order(-1)
        public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer(VeloProperties properties,
                                                                                           BeanFactory beanFactory) {
            return builder -> {
                VeloProperties.JacksonProperties jacksonProperties = properties.getJackson();

                // 先收口基础时间与容错策略，再让业务自定义器继续叠加更细粒度的能力。
                builder
                        .failOnEmptyBeans(false)
                        .failOnUnknownProperties(false);

                if (jacksonProperties.isDateTimeEnabled()) {
                    String dateTimeFormat = properties.getDateTimeFormat().getDateTime();
                    String dateFormat = properties.getDateTimeFormat().getDate();
                    String timeFormat = properties.getDateTimeFormat().getTime();
                    String timeZoneId = properties.getDateTimeFormat().getTimeZone();
                    TimeZone timeZone = TimeZone.getTimeZone(ZoneId.of(timeZoneId));
                    FlexibleDateFormat defaultDateFormat = new FlexibleDateFormat(dateTimeFormat, dateFormat, timeZone);
                    DateTimeFormatter dateTimeFormatter = new DateTimeFormatterFactory(dateTimeFormat).createDateTimeFormatter();
                    DateTimeFormatter dateFormatter = new DateTimeFormatterFactory(dateFormat).createDateTimeFormatter();
                    DateTimeFormatter timeFormatter = new DateTimeFormatterFactory(timeFormat).createDateTimeFormatter();

                    builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                    builder.dateFormat(defaultDateFormat)
                            .timeZone(timeZone);

                    builder.deserializerByType(LocalDateTime.class,
                                    new LocalDateTimeDeserializer(dateTimeFormatter))
                            .deserializerByType(LocalDate.class,
                                    new LocalDateDeserializer(dateFormatter))
                            .deserializerByType(LocalTime.class,
                                    new LocalTimeDeserializer(timeFormatter));
                    builder.serializers(new LocalDateTimeSerializer(dateTimeFormatter))
                            .serializers(new LocalDateSerializer(dateFormatter))
                            .serializers(new LocalTimeSerializer(timeFormatter));
                }

                if (jacksonProperties.isSerializeLongAsString()) {
                    builder.postConfigurer(mapper -> {
                        setStringDefault(mapper, Long.class);
                        setStringDefault(mapper, Long.TYPE);
                        setStringDefault(mapper, BigInteger.class);
                    });
                    builder.serializerByType(long[].class, new NumericArraySerializer<>(long[].class, Long.class));
                }
                if (jacksonProperties.isSerializeBigDecimalAsString() || jacksonProperties.isBigDecimalStripTrailingZeros()) {
                    builder.serializerByType(BigDecimal.class, new ConfigurableBigDecimalSerializer(
                            jacksonProperties.isSerializeBigDecimalAsString(),
                            jacksonProperties.isBigDecimalStripTrailingZeros()));
                }
                if (jacksonProperties.isSerializeFloatingAsString()) {
                    builder.postConfigurer(mapper -> {
                        setStringDefault(mapper, Double.class);
                        setStringDefault(mapper, Double.TYPE);
                        setStringDefault(mapper, Float.class);
                        setStringDefault(mapper, Float.TYPE);
                    });
                    builder.serializerByType(double[].class, new NumericArraySerializer<>(double[].class, Double.class))
                            .serializerByType(float[].class, new NumericArraySerializer<>(float[].class, Float.class));
                }

                ObjectProvider<JsonProcessorProvider> jsonProcessorProviderObjectProvider = beanFactory
                        .getBeanProvider(JsonProcessorProvider.class);
                jsonProcessorProviderObjectProvider.ifAvailable(bean -> {
                    XssCleaner xssCleaner = null;
                    if (properties.getXss().isJacksonEnabled()) {
                        ObjectProvider<XssCleaner> xssCleanerObjectProvider = beanFactory
                                .getBeanProvider(XssCleaner.class);
                        xssCleaner = xssCleanerObjectProvider.getIfAvailable();
                    }

                    // String 处理器始终注册，才能让带 @JsonEncode/@JsonDecode 的字段生效；
                    // 无注解字段不执行转换，XSS 清洗则由独立的 jackson-enabled 控制。
                    builder.deserializerByType(String.class,
                            new JacksonStringDeserializer(bean, xssCleaner));
                    builder.serializerByType(String.class, new JacksonStringSerializer(bean));
                });

            };
        }

        private static void setStringDefault(ObjectMapper mapper, Class<?> type) {
            JsonFormat.Value current = mapper.configOverride(type).getFormat();
            JsonFormat.Value defaultFormat = JsonFormat.Value.forShape(JsonFormat.Shape.STRING);
            mapper.configOverride(type).setFormat(defaultFormat.withOverrides(current));
        }
    }

}
