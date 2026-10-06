package io.github.luminion.velo.jackson;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.converter.datetime.FlexibleDateFormat;
import io.github.luminion.velo.jackson.deserializer.JacksonStringDeserializer;
import io.github.luminion.velo.jackson.serializer.ConfigurableBigDecimalSerializer;
import io.github.luminion.velo.jackson.serializer.JacksonStringSerializer;
import io.github.luminion.velo.jackson.serializer.JsonEnumSerializerModifier;
import io.github.luminion.velo.jackson.serializer.NumericArraySerializer;
import io.github.luminion.velo.spi.JsonProcessorProvider;
import io.github.luminion.velo.xss.XssCleaner;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.module.SimpleModule;

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
 * Jackson 3 自动配置。
 *
 * Boot 4 的 Jackson SPI 与 Boot 2/3 不兼容，这里使用 Jackson 3 API 对齐 starter 的通用 JSON 行为。
 */
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
@ConditionalOnProperty(prefix = "velo.jackson", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloJacksonAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    static class JsonMapperBuilderCustomizerConfiguration {

        @Bean
        @Order(-1)
        public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer(VeloProperties properties,
                                                                       BeanFactory beanFactory) {
            return builder -> {
                VeloProperties.JacksonProperties jacksonProperties = properties.getJackson();

                builder.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
                builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

                SimpleModule module = new SimpleModule("velo-jackson3");
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

                    builder.defaultDateFormat(defaultDateFormat);
                    builder.defaultTimeZone(timeZone);

                    module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
                    module.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
                    module.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
                    module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
                    module.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
                    module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));
                }

                if (jacksonProperties.isSerializeLongAsString()) {
                    setStringDefault(builder, Long.class);
                    setStringDefault(builder, Long.TYPE);
                    setStringDefault(builder, BigInteger.class);
                    module.addSerializer(long[].class, new NumericArraySerializer<>(long[].class, Long.class));
                }
                if (jacksonProperties.isSerializeBigDecimalAsString() || jacksonProperties.isBigDecimalStripTrailingZeros()) {
                    module.addSerializer(BigDecimal.class, new ConfigurableBigDecimalSerializer(
                            jacksonProperties.isSerializeBigDecimalAsString(),
                            jacksonProperties.isBigDecimalStripTrailingZeros()));
                }
                if (jacksonProperties.isSerializeFloatingAsString()) {
                    setStringDefault(builder, Double.class);
                    setStringDefault(builder, Double.TYPE);
                    setStringDefault(builder, Float.class);
                    setStringDefault(builder, Float.TYPE);
                    module.addSerializer(double[].class, new NumericArraySerializer<>(double[].class, Double.class));
                    module.addSerializer(float[].class, new NumericArraySerializer<>(float[].class, Float.class));
                }
                ObjectProvider<JsonProcessorProvider> jsonProcessorProviderObjectProvider = beanFactory
                        .getBeanProvider(JsonProcessorProvider.class);
                jsonProcessorProviderObjectProvider.ifAvailable(bean -> {
                    XssCleaner xssCleaner = null;
                    if (properties.getXss().isJacksonEnabled()) {
                        ObjectProvider<XssCleaner> xssCleanerObjectProvider = beanFactory.getBeanProvider(XssCleaner.class);
                        xssCleaner = xssCleanerObjectProvider.getIfAvailable();
                    }
                    // String 处理器始终注册，才能让带 @JsonEncode/@JsonDecode 的字段生效；
                    // 无注解字段不执行转换，XSS 清洗则由独立的 jackson-enabled 控制。
                    module.addDeserializer(String.class, new JacksonStringDeserializer(bean, xssCleaner));
                    module.addSerializer(String.class, new JacksonStringSerializer(bean));
                });
                if (jacksonProperties.isEnumDescEnabled()) {
                    module.setSerializerModifier(new JsonEnumSerializerModifier(jacksonProperties));
                }
                builder.addModule(module);
            };
        }

        private static void setStringDefault(tools.jackson.databind.json.JsonMapper.Builder builder, Class<?> type) {
            builder.withConfigOverride(type, override -> {
                JsonFormat.Value defaultFormat = JsonFormat.Value.forShape(JsonFormat.Shape.STRING);
                override.setFormat(defaultFormat.withOverrides(override.getFormat()));
            });
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnProperty(prefix = "velo.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class JacksonRedisConfiguration {

        @Bean
        @ConditionalOnMissingBean(RedisSerializer.class)
        public RedisSerializer<Object> redisSerializer() {
            return RedisSerializer.json();
        }
    }
}
