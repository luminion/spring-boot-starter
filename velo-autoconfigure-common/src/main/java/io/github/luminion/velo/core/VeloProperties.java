package io.github.luminion.velo.core;

import io.github.luminion.velo.xss.XssStrategy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.logging.LogLevel;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central configuration properties for the starter.
 *
 * <p>Only this class keeps English comments so that generated metadata stays readable
 * across different IDE and terminal encodings.</p>
 */
@Data
@ConfigurationProperties("velo")
public class VeloProperties {

    /**
     * Prefix used by rate-limit related keys.
     */
    private String rateLimitPrefix = "rateLimit:";

    /**
     * Prefix used by idempotent related keys.
     */
    private String idempotentPrefix = "idempotent:";

    /**
     * Prefix used by lock related keys.
     */
    private String lockPrefix = "lock:";

    /**
     * Default log level used by starter managed log components.
     */
    private LogLevel logLevel = LogLevel.INFO;

    /**
     * Default XSS cleaning strategy.
     */
    private XssStrategy xssStrategy = XssStrategy.RELAXED;

    /**
     * Candidate field names used to resolve enum code values.
     */
    private List<String> enumCodeFields = Arrays.asList("code", "id", "value");

    /**
     * Candidate field names used to resolve enum description values.
     */
    private List<String> enumDescFields = Arrays.asList("desc", "name", "label");

    /**
     * Date and time formatting settings shared by web, Jackson and Excel features.
     */
    private DateTimeFormatProperties dateTimeFormat = new DateTimeFormatProperties();

    /**
     * Core infrastructure settings.
     */
    private CoreProperties core = new CoreProperties();

    /**
     * Idempotent feature settings.
     */
    private IdempotentProperties idempotent = new IdempotentProperties();

    /**
     * Rate-limit feature settings.
     */
    private RateLimitProperties rateLimit = new RateLimitProperties();

    /**
     * Lock feature settings.
     */
    private LockProperties lock = new LockProperties();

    /**
     * Redis helper settings.
     */
    private RedisProperties redis = new RedisProperties();

    /**
     * Cache feature settings.
     */
    private CacheProperties cache = new CacheProperties();

    /**
     * Excel integration settings.
     */
    private ExcelProperties excel = new ExcelProperties();

    /**
     * Jackson integration settings.
     */
    private JacksonProperties jackson = new JacksonProperties();

    /**
     * Log auto-configuration settings.
     */
    private LogProperties log = new LogProperties();

    /**
     * MyBatis-Plus integration settings.
     */
    private MybatisPlusProperties mybatisPlus = new MybatisPlusProperties();

    /**
     * Web related settings.
     */
    private WebProperties web = new WebProperties();

    @Data
    public static class CoreProperties {

        /**
         * Enables core auto-configuration.
         */
        private boolean enabled = true;

        /**
         * Enables the default fingerprinter bean.
         */
        private boolean fingerprinterEnabled = true;

        /**
         * Enables the default naming suffix strategy bean.
         */
        private boolean namingSuffixStrategyEnabled = true;

        /**
         * Enables the default enum field convention bean.
         */
        private boolean enumFieldConventionEnabled = true;

        /**
         * Enables the default JSON processor provider bean.
         */
        private boolean jsonProcessorProviderEnabled = true;
    }

    @Data
    public static class IdempotentProperties {

        /**
         * Enables idempotent auto-configuration.
         */
        private boolean enabled = true;

        /**
         * Enables the idempotent aspect bean.
         */
        private boolean aspectEnabled = true;

        /**
         * Backend implementation used by idempotent handler selection.
         */
        private ConcurrencyBackend backend = ConcurrencyBackend.AUTO;
    }

    @Data
    public static class RateLimitProperties {

        /**
         * Enables rate-limit auto-configuration.
         */
        private boolean enabled = true;

        /**
         * Enables the rate-limit aspect bean.
         */
        private boolean aspectEnabled = true;

        /**
         * Backend implementation used by rate-limit handler selection.
         */
        private ConcurrencyBackend backend = ConcurrencyBackend.AUTO;
    }

    @Data
    public static class LockProperties {

        /**
         * Enables lock auto-configuration.
         */
        private boolean enabled = true;

        /**
         * Enables the lock aspect bean.
         */
        private boolean aspectEnabled = true;

        /**
         * Backend implementation used by lock handler selection.
         */
        private ConcurrencyBackend backend = ConcurrencyBackend.AUTO;
    }

    @Data
    public static class RedisProperties {

        /**
         * Enables Redis helper auto-configuration.
         */
        private boolean enabled = true;

        /**
         * Enables the `stringObjectRedisTemplate` bean.
         */
        private boolean stringObjectRedisTemplateEnabled = true;

        /**
         * Enables the customized `redisTemplate` bean.
         */
        private boolean redisTemplateEnabled = true;
    }

    @Data
    public static class CacheProperties {

        /**
         * Enables cache auto-configuration.
         */
        private boolean enabled = true;

        /**
         * Static prefix added to cache keys.
         */
        private String keyPrefix = "";

        /**
         * Separator used when building cache key prefixes.
         */
        private String keySeparator = ":";

        /**
         * Default cache TTL in seconds.
         */
        private int defaultTtlSeconds = 3000;

        /**
         * Per-cache TTL overrides in seconds.
         */
        private Map<String, Integer> ttlMap = new LinkedHashMap<>();

        /**
         * Enables the Redis cache TTL map provider bean.
         */
        private boolean redisCacheTimeMapProviderEnabled = true;

        /**
         * Enables the Redis cache configuration bean.
         */
        private boolean redisCacheConfigurationEnabled = true;

        /**
         * Enables the Redis cache manager bean.
         */
        private boolean cacheManagerEnabled = true;
    }

    @Data
    public static class ExcelProperties {

        /**
         * Enables Excel auto-configuration.
         */
        private boolean enabled = true;

        /**
         * Fine-grained converter switches shared by EasyExcel, FastExcel and Fesod.
         */
        private ConverterProperties converters = new ConverterProperties();

        @Data
        public static class ConverterProperties {

            /**
             * Enables automatic registration of built-in Excel converters.
             */
            private boolean enabled = false;

            /**
             * Enables the Boolean Excel converter.
             */
            private boolean booleanConverterEnabled = true;

            /**
             * Enables the Long Excel converter.
             */
            private boolean longConverterEnabled = true;

            /**
             * Enables the Float Excel converter.
             */
            private boolean floatConverterEnabled = true;

            /**
             * Enables the Double Excel converter.
             */
            private boolean doubleConverterEnabled = true;

            /**
             * Enables the BigInteger Excel converter.
             */
            private boolean bigIntegerConverterEnabled = true;

            /**
             * Enables the BigDecimal Excel converter.
             */
            private boolean bigDecimalConverterEnabled = true;

            /**
             * Enables the java.util.Date Excel converter.
             */
            private boolean dateConverterEnabled = true;

            /**
             * Enables the java.sql.Timestamp Excel converter.
             */
            private boolean sqlTimestampConverterEnabled = true;

            /**
             * Enables the java.sql.Date Excel converter.
             */
            private boolean sqlDateConverterEnabled = true;

            /**
             * Enables the java.sql.Time Excel converter.
             */
            private boolean sqlTimeConverterEnabled = true;

            /**
             * Enables the LocalDateTime Excel converter.
             */
            private boolean localDateTimeConverterEnabled = true;

            /**
             * Enables the LocalDate Excel converter.
             */
            private boolean localDateConverterEnabled = true;

            /**
             * Enables the LocalTime Excel converter.
             */
            private boolean localTimeConverterEnabled = true;
        }
    }

    @Data
    public static class JacksonProperties {

        /**
         * Enables Jackson auto-configuration.
         */
        private boolean enabled = true;

        /**
         * Enables the Jackson builder customizer bean.
         */
        private boolean builderCustomizerEnabled = true;

        /**
         * Enables the Redis serializer bean provided by the Jackson auto-configuration.
         */
        private boolean redisSerializerEnabled = true;

        /**
         * Date-time conversion settings for Jackson customization.
         */
        private DateTimeProperties dateTime = new DateTimeProperties();

        /**
         * Serializes integer values as strings only when they are outside the JavaScript safe integer range.
         */
        private boolean writeUnsafeIntegerAsString = true;

        /**
         * Serializes BigDecimal values as strings.
         */
        private boolean writeBigDecimalAsString = true;

        /**
         * Serializes float and double values as strings.
         */
        private boolean writeFloatingPointAsString = false;

        /**
         * Adds enum description fields during serialization.
         */
        private boolean enumDescriptionEnabled = false;

        /**
         * Controls automatic registration of custom String serializer and deserializer hooks.
         */
        private StringConverterProperties stringConverters = new StringConverterProperties();

        @Data
        public static class DateTimeProperties {

            /**
             * Enables date-time related Jackson customization.
             */
            private boolean enabled = true;

            /**
             * Enables applying the configured default legacy date format and time zone.
             */
            private boolean javaUtilDateEnabled = true;

            /**
             * Enables LocalDate, LocalDateTime and LocalTime serializers.
             */
            private boolean serializersEnabled = true;

            /**
             * Enables LocalDate, LocalDateTime and LocalTime deserializers.
             */
            private boolean deserializersEnabled = true;
        }

        @Data
        public static class StringConverterProperties {

            /**
             * Enables automatic registration of starter managed String converters.
             */
            private boolean enabled = false;

            /**
             * Enables automatic registration of the custom String serializer.
             */
            private boolean serializerEnabled = true;

            /**
             * Enables automatic registration of the custom String deserializer.
             */
            private boolean deserializerEnabled = true;
        }
    }

    @Data
    public static class LogProperties {

        /**
         * Enables log auto-configuration.
         */
        private boolean enabled = true;

        /**
         * Enables the default SLF4J log writer bean.
         */
        private boolean slf4jLogWriterEnabled = true;

        /**
         * Enables the method argument logging aspect.
         */
        private boolean argsAspectEnabled = true;

        /**
         * Enables the method result logging aspect.
         */
        private boolean resultAspectEnabled = true;

        /**
         * Enables the error logging aspect.
         */
        private boolean errorAspectEnabled = true;

        /**
         * Enables the slow-call logging aspect.
         */
        private boolean slowAspectEnabled = true;
    }

    @Data
    public static class WebProperties {

        /**
         * Enables web MVC auto-configuration.
         */
        private boolean enabled = true;

        /**
         * Enables the MVC configurer bean provided by this starter.
         */
        private boolean mvcConfigurerEnabled = true;

        /**
         * Enables automatic registration of web date-time formatters.
         */
        private boolean dateTimeFormatterRegistrationEnabled = true;

        /**
         * Enables automatic registration of the XSS String converter into the MVC conversion service.
         */
        private boolean xssStringConverterRegistrationEnabled = true;

        /**
         * Enables permissive CORS handling in the MVC configurer.
         */
        private boolean allowCors;

        /**
         * Controller request logging settings.
         */
        private RequestLoggingProperties requestLogging = new RequestLoggingProperties();

        /**
         * Web XSS settings.
         */
        private XssProperties xss = new XssProperties();
    }

    @Data
    public static class RequestLoggingProperties {

        /**
         * Enables the controller request logging aspect.
         */
        private boolean enabled = true;
    }

    @Data
    public static class XssProperties {

        /**
         * Enables web XSS protection.
         */
        private boolean enabled;

        /**
         * Enables the default XSS cleaner bean.
         */
        private boolean cleanerEnabled = true;

        /**
         * Enables the web layer String converter backed by the XSS cleaner.
         */
        private boolean stringConverterEnabled = true;
    }

    @Data
    public static class DateTimeFormatProperties {

        /**
         * Enables date and time formatter auto-configuration.
         */
        private boolean enabled = true;

        /**
         * Enables automatic registration of web date-time converters.
         */
        private ConverterProperties converters = new ConverterProperties();

        /**
         * Default time pattern.
         */
        private String time = "HH:mm:ss";

        /**
         * Default date pattern.
         */
        private String date = "yyyy-MM-dd";

        /**
         * Default date-time pattern.
         */
        private String dateTime = "yyyy-MM-dd HH:mm:ss";

        /**
         * Default time zone used by date based converters and serializers.
         */
        private String timeZone = "GMT+8";

        @Data
        public static class ConverterProperties {

            /**
             * Enables automatic registration of built-in date-time converters.
             */
            private boolean enabled = false;

            /**
             * Enables the java.util.Date converter.
             */
            private boolean javaUtilDateConverterEnabled = true;

            /**
             * Enables the LocalDateTime converter.
             */
            private boolean localDateTimeConverterEnabled = true;

            /**
             * Enables the LocalDate converter.
             */
            private boolean localDateConverterEnabled = true;

            /**
             * Enables the LocalTime converter.
             */
            private boolean localTimeConverterEnabled = true;

            /**
             * Enables the java.sql.Date converter.
             */
            private boolean sqlDateConverterEnabled = true;

            /**
             * Enables the java.sql.Time converter.
             */
            private boolean sqlTimeConverterEnabled = true;

            /**
             * Enables the java.sql.Timestamp converter.
             */
            private boolean sqlTimestampConverterEnabled = true;
        }
    }

    @Data
    public static class MybatisPlusProperties {

        /**
         * Enables MyBatis-Plus auto-configuration.
         */
        private boolean enabled = true;

        /**
         * Enables the MyBatis-Plus interceptor bean.
         */
        private boolean interceptorEnabled = true;

        /**
         * Enables the pagination inner interceptor bean.
         */
        private boolean paginationInnerInterceptorEnabled = true;

        /**
         * Enables the optimistic locker inner interceptor bean.
         */
        private boolean optimisticLockerInnerInterceptorEnabled = true;

        /**
         * Enables the block attack inner interceptor bean.
         */
        private boolean blockAttackInnerInterceptorEnabled = true;
    }
}
