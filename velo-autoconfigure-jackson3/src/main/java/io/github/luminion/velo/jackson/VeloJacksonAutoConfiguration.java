package io.github.luminion.velo.jackson;

import io.github.luminion.velo.core.VeloProperties;
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
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;
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
 * Boot 4 的 Jackson SPI 与 Boot 2/3 不兼容，这里仅保留基础时间格式、数字转字符串和 Redis 序列化配置。
 */
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
@ConditionalOnProperty(prefix = "velo.jackson", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloJacksonAutoConfiguration {

    private static final long MAX_SAFE_INTEGER = 9007199254740991L;

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "velo.jackson", name = "builder-customizer-enabled", havingValue = "true", matchIfMissing = true)
    static class JsonMapperBuilderCustomizerConfiguration {

        @Bean
        @Order(-1)
        public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer(VeloProperties properties) {
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

                builder.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
                builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                if (jacksonDateTime.isEnabled()) {
                    if (jacksonDateTime.isJavaUtilDateEnabled()) {
                        builder.defaultDateFormat(simpleDateFormat);
                        builder.defaultTimeZone(timeZone);
                    }
                    builder.findAndAddModules();
                }

                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormat);
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(dateFormat);
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(timeFormat);

                SimpleModule module = new SimpleModule("velo-jackson3");
                if (jacksonProperties.isWriteIntegerAsString() || jacksonProperties.isWriteUnsafeIntegerAsString()) {
                    StdSerializer<Long> longSerializer = longJsonSerializer(jacksonProperties);
                    module.addSerializer(Long.class, longSerializer);
                    module.addSerializer(Long.TYPE, longSerializer);
                    module.addSerializer(BigInteger.class, bigIntegerJsonSerializer(jacksonProperties));
                }
                if (jacksonProperties.isWriteBigDecimalAsString()) {
                    module.addSerializer(BigDecimal.class, ToStringSerializer.instance);
                }
                if (jacksonProperties.isWriteFloatingPointAsString()) {
                    module.addSerializer(Double.class, ToStringSerializer.instance);
                    module.addSerializer(Double.TYPE, ToStringSerializer.instance);
                    module.addSerializer(Float.class, ToStringSerializer.instance);
                    module.addSerializer(Float.TYPE, ToStringSerializer.instance);
                }
                if (jacksonDateTime.isEnabled() && jacksonDateTime.isSerializersEnabled()) {
                    module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
                    module.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
                    module.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
                }
                if (jacksonDateTime.isEnabled() && jacksonDateTime.isDeserializersEnabled()) {
                    module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
                    module.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
                    module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));
                }
                builder.addModule(module);
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnProperty(prefix = "velo.jackson", name = "redis-serializer-enabled", havingValue = "true", matchIfMissing = true)
    static class JacksonRedisConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "redisSerializer")
        public RedisSerializer<Object> redisSerializer() {
            return RedisSerializer.json();
        }
    }

    private static StdSerializer<Long> longJsonSerializer(final VeloProperties.JacksonProperties properties) {
        return new StdSerializer<Long>(Long.class) {
            @Override
            public void serialize(Long value, JsonGenerator gen, SerializationContext ctxt) {
                if (value == null) {
                    gen.writeNull();
                    return;
                }
                if (shouldSerializeIntegerAsString(BigInteger.valueOf(value.longValue()), properties)) {
                    gen.writeString(Long.toString(value));
                    return;
                }
                gen.writeNumber(value.longValue());
            }
        };
    }

    private static StdSerializer<BigInteger> bigIntegerJsonSerializer(final VeloProperties.JacksonProperties properties) {
        return new StdSerializer<BigInteger>(BigInteger.class) {
            @Override
            public void serialize(BigInteger value, JsonGenerator gen, SerializationContext ctxt) {
                if (value == null) {
                    gen.writeNull();
                    return;
                }
                if (shouldSerializeIntegerAsString(value, properties)) {
                    gen.writeString(value.toString());
                    return;
                }
                gen.writeNumber(value);
            }
        };
    }

    private static boolean shouldSerializeIntegerAsString(BigInteger value, VeloProperties.JacksonProperties properties) {
        if (properties.isWriteIntegerAsString()) {
            return true;
        }
        return properties.isWriteUnsafeIntegerAsString() && isUnsafeInteger(value);
    }

    private static boolean isUnsafeInteger(BigInteger value) {
        return value.abs().compareTo(BigInteger.valueOf(MAX_SAFE_INTEGER)) > 0;
    }
}
