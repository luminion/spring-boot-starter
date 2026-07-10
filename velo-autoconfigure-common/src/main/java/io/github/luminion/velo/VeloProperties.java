package io.github.luminion.velo;

import io.github.luminion.velo.core.VeloAdvisorOrder;
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
     * Starter default behavior profile.
     */
    private VeloMode mode = VeloMode.OPINIONATED;

    /**
     * Startup banner settings.
     */
    private BannerProperties banner = new BannerProperties();

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

    /**
     * Feign related settings.
     */
    private FeignProperties feign = new FeignProperties();

    /**
     * Aspect execution order settings.
     */
    private AspectOrderProperties aspectOrder = new AspectOrderProperties();

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

        /**
         * Polling interval used by the simple Redis lock while waiting for acquisition.
         */
        private Duration retryInterval = Duration.ofMillis(10);
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
         * Whether to cache null values. When enabled, null results are cached
         * to prevent cache penetration. Default is true.
         */
        private boolean nullCachingEnabled = true;

        /**
         * Percentage of jitter applied to TTL values to prevent cache stampede.
         * For example, a value of 10 means each entry's TTL is randomly shifted by up to ±10%
         * when it is written, independently per key. Set to 0 to disable jitter. Default is 0.
         */
        private int ttlJitterPercentage;

        public void setTtlJitterPercentage(int ttlJitterPercentage) {
            if (ttlJitterPercentage < 0 || ttlJitterPercentage > 100) {
                throw new IllegalArgumentException("Cache TTL jitter percentage must be between 0 and 100.");
            }
            this.ttlJitterPercentage = ttlJitterPercentage;
        }

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
         * Serializes long values as strings on write, to avoid precision loss on the
         * front-end (JavaScript Number cannot safely represent integers beyond 2^53).
         * Only affects serialization (write); deserialization accepts both numbers and strings.
         */
        private boolean serializeLongAsString = true;

        /**
         * Serializes BigDecimal values as strings on write.
         */
        private boolean serializeBigDecimalAsString = true;

        /**
         * Removes trailing zeros before serializing BigDecimal values.
         */
        private boolean bigDecimalStripTrailingZeros = false;

        /**
         * Serializes float and double values as strings on write.
         */
        private boolean serializeFloatingAsString = false;

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

        /**
         * Trace id settings used by MDC, servlet responses and Feign propagation.
         */
        private TraceProperties trace = new TraceProperties();

        /**
         * Unified invocation logging settings.
         */
        private InvocationProperties invocation = new InvocationProperties();
    }

    @Data
    public static class TraceProperties {

        /**
         * Enables automatic trace id creation and propagation.
         */
        private boolean enabled = true;

        /**
         * Request and response header used for trace id propagation.
         */
        private String headerName = "X-Trace-Id";

        /**
         * MDC key used by logging frameworks.
         */
        private String mdcKey = "traceId";

        /**
         * Writes the trace id back to servlet responses.
         */
        private boolean responseHeaderEnabled = true;

        /**
         * Propagates the current trace id to Feign requests.
         */
        private boolean feignPropagationEnabled = true;

        /**
         * Adds trace id to Spring Boot default log level pattern when no pattern is customized.
         */
        private boolean loggingPatternEnabled = true;
    }

    @Data
    public static class InvocationProperties {

        /**
         * Enables unified invocation logging.
         */
        private boolean enabled = true;

        /**
         * Maximum length of logged argument and result payloads. Use -1 for unlimited output.
         */
        private int maxPayloadLength = -1;

        /**
         * Includes invocation arguments in logs.
         */
        private boolean includeArgs = true;

        /**
         * Includes invocation results in successful logs.
         */
        private boolean includeResult = true;

        /**
         * Includes stack traces in error logs.
         */
        private boolean includeErrorStackTrace;

        /**
         * Controller invocation logging settings.
         */
        private InvocationSourceProperties controller = new InvocationSourceProperties();

        /**
         * Feign invocation logging settings.
         */
        private InvocationSourceProperties feign = new InvocationSourceProperties();

        /**
         * Method invocation logging settings.
         */
        private InvocationSourceProperties method = new InvocationSourceProperties();
    }

    @Data
    public static class InvocationSourceProperties {

        /**
         * Enables this invocation log source.
         */
        private boolean enabled = true;
    }

    @Data
    public static class WebProperties {

        /**
         * Enables web MVC auto-configuration.
         */
        private boolean enabled = true;

        /**
         * Enables permissive CORS handling in the MVC configurer.
         * @deprecated use {@code velo.web.cors.enabled} instead.
         */
        @Deprecated
        private boolean allowCors;

        /**
         * CORS settings.
         */
        private CorsProperties cors = new CorsProperties();

        /**
         * Web XSS settings.
         */
        private XssProperties xss = new XssProperties();
    }

    @Data
    public static class CorsProperties {

        /**
         * Enables CORS handling in the MVC configurer.
         */
        private boolean enabled;

        /**
         * Comma-separated or array-style list of allowed origin patterns.
         * Defaults to {@code *} (all origins).
         */
        private String[] allowedOriginPatterns = {"*"};

        /**
         * Comma-separated or array-style list of allowed HTTP methods.
         */
        private String[] allowedMethods = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};

        /**
         * Whether to allow credentials (cookies, authorization headers).
         */
        private boolean allowCredentials = true;

        /**
         * Max age of preflight cache in seconds.
         */
        private long maxAge = 3600;
    }

    @Data
    public static class FeignProperties {

        /**
         * Enables Feign client logging auto-configuration.
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

    @Data
    public static class AspectOrderProperties {

        /**
         * Order for the idempotent aspect.
         */
        private int idempotent = VeloAdvisorOrder.CONCURRENCY_IDEMPOTENT;

        /**
         * Order for the rate-limit aspect.
         */
        private int rateLimit = VeloAdvisorOrder.CONCURRENCY_RATE_LIMIT;

        /**
         * Order for the lock aspect.
         */
        private int lock = VeloAdvisorOrder.CONCURRENCY_LOCK;

        /**
         * Order for the invoke-log aspect.
         */
        private int invokeLog = VeloAdvisorOrder.LOG_INVOKE;

        /**
         * Order for the slow-log aspect.
         */
        private int slowLog = VeloAdvisorOrder.LOG_SLOW;

        /**
         * Order for the controller-log aspect.
         */
        private int controllerLog = VeloAdvisorOrder.LOG_CONTROLLER;

        /**
         * Order for the feign-log aspect.
         */
        private int feignLog = VeloAdvisorOrder.LOG_FEIGN;
    }

    @Data
    public static class BannerProperties {

        /**
         * Prints the Velo startup banner with a summary of enabled features.
         * Disabled by default; enable to print the banner on startup.
         */
        private boolean enabled = false;
    }
}
