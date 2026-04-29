package io.github.luminion.velo.jackson;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.jackson.deserializer.JacksonStringDeserializer;
import io.github.luminion.velo.jackson.serializer.JacksonStringSerializer;
import io.github.luminion.velo.jackson.serializer.JsonEnumSerializerModifier;
import io.github.luminion.velo.jackson.serializer.UnsafeBigIntegerToStringSerializer;
import io.github.luminion.velo.jackson.serializer.UnsafeLongToStringSerializer;
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
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
                String dateTimeFormat = properties.getDateTimeFormat().getDateTime();
                String dateFormat = properties.getDateTimeFormat().getDate();
                String timeFormat = properties.getDateTimeFormat().getTime();
                String timeZoneId = properties.getDateTimeFormat().getTimeZone();
                TimeZone timeZone = TimeZone.getTimeZone(timeZoneId);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateTimeFormat);
                simpleDateFormat.setTimeZone(timeZone);
                VeloProperties.JacksonProperties jacksonProperties = properties.getJackson();

                builder.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
                builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                if (jacksonProperties.isDateTimeEnabled()) {
                    builder.defaultDateFormat(simpleDateFormat);
                    builder.defaultTimeZone(timeZone);
                    builder.findAndAddModules();
                }

                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat);
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(dateFormat);
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(timeFormat);

                SimpleModule module = new SimpleModule("velo-jackson3");
                if (jacksonProperties.isUnsafeIntegerAsString()) {
                    UnsafeLongToStringSerializer longSerializer = new UnsafeLongToStringSerializer();
                    module.addSerializer(Long.class, longSerializer);
                    module.addSerializer(Long.TYPE, longSerializer);
                    module.addSerializer(BigInteger.class, new UnsafeBigIntegerToStringSerializer());
                }
                if (jacksonProperties.isBigDecimalAsString()) {
                    module.addSerializer(BigDecimal.class, ToStringSerializer.instance);
                }
                if (jacksonProperties.isFloatingAsString()) {
                    module.addSerializer(Double.class, ToStringSerializer.instance);
                    module.addSerializer(Double.TYPE, ToStringSerializer.instance);
                    module.addSerializer(Float.class, ToStringSerializer.instance);
                    module.addSerializer(Float.TYPE, ToStringSerializer.instance);
                }
                if (jacksonProperties.isDateTimeEnabled()) {
                    module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
                    module.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
                    module.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
                }
                if (jacksonProperties.isDateTimeEnabled()) {
                    module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
                    module.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
                    module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));
                }
                if (jacksonProperties.isStringConverterEnabled()) {
                    ObjectProvider<JsonProcessorProvider> jsonProcessorProviderObjectProvider = beanFactory
                            .getBeanProvider(JsonProcessorProvider.class);
                    jsonProcessorProviderObjectProvider.ifAvailable(bean -> {
                        ObjectProvider<XssCleaner> xssCleanerObjectProvider = beanFactory.getBeanProvider(XssCleaner.class);
                        XssCleaner xssCleaner = xssCleanerObjectProvider.getIfAvailable();
                        module.addDeserializer(String.class, new JacksonStringDeserializer(bean, xssCleaner));
                        module.addSerializer(String.class, new JacksonStringSerializer(bean));
                    });
                }
                if (jacksonProperties.isEnumDescEnabled()) {
                    module.setSerializerModifier(new JsonEnumSerializerModifier(jacksonProperties));
                }
                builder.addModule(module);
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RedisTemplate.class)
    static class JacksonRedisConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "redisSerializer")
        public RedisSerializer<Object> redisSerializer() {
            return RedisSerializer.json();
        }
    }
}
