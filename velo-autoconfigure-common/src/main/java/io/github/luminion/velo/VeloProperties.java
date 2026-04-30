package io.github.luminion.velo;

import io.github.luminion.velo.xss.XssStrategy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.logging.LogLevel;

import java.time.Duration;
import java.util.LinkedHashMap;
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
     * Date and time formatting settings shared by web, Jackson and Excel features.
     */
    private DateTimeFormatProperties dateTimeFormat = new DateTimeFormatProperties();

    /**
     * Spring converter settings.
     */
    private SpringConverterProperties springConverter = new SpringConverterProperties();

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
    public static class IdempotentProperties {

        /**
         * Enables idempotent auto-configuration.
         */
        private boolean enabled = true;

        /**
         * Backend implementation used by idempotent handler selection.
         */
        private ConcurrencyBackend backend = ConcurrencyBackend.AUTO;

        /**
         * Prefix used by idempotent related keys.
         */
        private String prefix = "idempotent:";
    }

    @Data
    public static class RateLimitProperties {

        /**
         * Enables rate-limit auto-configuration.
         */
        private boolean enabled = true;

        /**
         * Backend implementation used by rate-limit handler selection.
         */
        private ConcurrencyBackend backend = ConcurrencyBackend.AUTO;

        /**
         * Prefix used by rate-limit related keys.
         */
        private String prefix = "rateLimit:";
    }

    @Data
    public static class LockProperties {

        /**
         * Enables lock auto-configuration.
         */
        private boolean enabled = true;

        /**
         * Backend implementation used by lock handler selection.
         */
        private ConcurrencyBackend backend = ConcurrencyBackend.AUTO;

        /**
         * Prefix used by lock related keys.
         */
        private String prefix = "lock:";
    }

    @Data
    public static class RedisProperties {

        /**
         * Enables Redis helper auto-configuration.
         */
        private boolean enabled = true;
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
        private String prefix = "";

        /**
         * Separator used when building cache key prefixes.
         */
        private String separator = ":";

        /**
         * Default cache TTL.
         */
        private Duration defaultTtl = Duration.ofMinutes(5);

        /**
         * Per-cache TTL overrides.
         */
        private Map<String, Duration> ttl = new LinkedHashMap<>();

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
            private boolean enabled = true;

            /**
             * Enables the Boolean Excel converter.
             */
            private boolean booleanEnabled = true;

            /**
             * Enables the Long Excel converter.
             */
            private boolean longEnabled = true;

            /**
             * Enables the Float Excel converter.
             */
            private boolean floatEnabled = true;

            /**
             * Enables the Double Excel converter.
             */
            private boolean doubleEnabled = true;

            /**
             * Enables the BigInteger Excel converter.
             */
            private boolean bigIntegerEnabled = true;

            /**
             * Enables the BigDecimal Excel converter.
             */
            private boolean bigDecimalEnabled = true;

            /**
             * Enables the java.util.Date Excel converter.
             */
            private boolean dateEnabled = true;

            /**
             * Enables the LocalDateTime Excel converter.
             */
            private boolean localDateTimeEnabled = true;

            /**
             * Enables the LocalDate Excel converter.
             */
            private boolean localDateEnabled = true;

            /**
             * Enables the LocalTime Excel converter.
             */
            private boolean localTimeEnabled = true;
        }
    }

    @Data
    public static class JacksonProperties {

        /**
         * Enables Jackson auto-configuration.
         */
        private boolean enabled = true;

        /**
         * Enables date-time related Jackson customization.
         */
        private boolean dateTimeEnabled = true;

        /**
         * Serializes integer values as strings only when they are outside the JavaScript safe integer range.
         */
        private boolean unsafeIntegerAsString = true;

        /**
         * Serializes BigDecimal values as strings.
         */
        private boolean bigDecimalAsString = true;

        /**
         * Serializes float and double values as strings.
         */
        private boolean floatingAsString = false;

        /**
         * Adds enum description fields during serialization.
         */
        private boolean enumDescEnabled = true;

        /**
         * Default suffix used by derived enum description fields.
         */
        private String enumNameSuffix = "name";

        /**
         * Candidate enum code-to-name field pairs, matched in declaration order.
         */
        private Map<String, String> enumMappings = defaultEnumMappings();

        /**
         * Enables automatic registration of starter managed String converters.
         */
        private boolean stringConverterEnabled = true;

        private static Map<String, String> defaultEnumMappings() {
            Map<String, String> mappings = new LinkedHashMap<>();
            mappings.put("code", "name");
            mappings.put("key", "value");
            return mappings;
        }
    }

    @Data
    public static class LogProperties {

        /**
         * Enables log auto-configuration.
         */
        private boolean enabled = true;

        /**
         * Default log level used by starter managed log components.
         */
        private LogLevel level = LogLevel.INFO;
    }

    @Data
    public static class WebProperties {

        /**
         * Enables web MVC auto-configuration.
         */
        private boolean enabled = true;

        /**
         * Enables permissive CORS handling in the MVC configurer.
         */
        private boolean allowCors;

        /**
         * Enables the controller request logging aspect.
         */
        private boolean requestLoggingEnabled = true;

        /**
         * Web XSS settings.
         */
        private XssProperties xss = new XssProperties();
    }

    @Data
    public static class XssProperties {

        /**
         * Enables web XSS protection.
         */
        private boolean enabled;

        /**
         * Default XSS cleaning strategy.
         */
        private XssStrategy strategy = XssStrategy.RELAXED;
    }

    @Data
    public static class DateTimeFormatProperties {
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

    }

    @Data
    public static class SpringConverterProperties {

        /**
         * Enables automatic registration of built-in date-time converters.
         */
        private boolean dateTimeEnabled = true;
    }

    @Data
    public static class MybatisPlusProperties {

        /**
         * Enables MyBatis-Plus auto-configuration.
         */
        private boolean enabled = true;

        /**
         * Enables the pagination inner interceptor bean.
         */
        private boolean paginationEnabled = true;

        /**
         * Enables the optimistic locker inner interceptor bean.
         */
        private boolean optimisticLockerEnabled = true;

        /**
         * Enables the block attack inner interceptor bean.
         */
        private boolean blockAttackEnabled = true;
    }
}
