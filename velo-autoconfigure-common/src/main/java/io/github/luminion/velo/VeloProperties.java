package io.github.luminion.velo;

import io.github.luminion.velo.core.VeloAdvisorOrder;
import io.github.luminion.velo.xss.XssStrategy;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.logging.LogLevel;

/** Starter 的统一配置属性。 */
@Data
@ConfigurationProperties("velo")
public class VeloProperties {

  /** 是否启用开箱即用的默认增强；设置为 {@code false} 时使用无侵入模式。 */
  private boolean opinionated = true;

  /** Startup banner settings. */
  private BannerProperties banner = new BannerProperties();

  /** Date and time formatting settings shared by web, Jackson and Excel features. */
  private DateTimeFormatProperties dateTimeFormat = new DateTimeFormatProperties();

  /** Spring converter settings. */
  private SpringConverterProperties springConverter = new SpringConverterProperties();

  /** Idempotent feature settings. */
  private IdempotentProperties idempotent = new IdempotentProperties();

  /** Rate-limit feature settings. */
  private RateLimitProperties rateLimit = new RateLimitProperties();

  /** Lock feature settings. */
  private LockProperties lock = new LockProperties();

  /** Redis helper settings. */
  private RedisProperties redis = new RedisProperties();

  /** Cache feature settings. */
  private CacheProperties cache = new CacheProperties();

  /** Excel integration settings. */
  private ExcelProperties excel = new ExcelProperties();

  /** Jackson integration settings. */
  private JacksonProperties jackson = new JacksonProperties();

  /** XSS 清洗策略与适配目标设置。 */
  private XssProperties xss = new XssProperties();

  /** Log auto-configuration settings. */
  private LogProperties log = new LogProperties();

  /** MyBatis-Plus integration settings. */
  private MybatisPlusProperties mybatisPlus = new MybatisPlusProperties();

  /** Web related settings. */
  private WebProperties web = new WebProperties();

  /** Feign related settings. */
  private FeignProperties feign = new FeignProperties();

  /** Aspect execution order settings. */
  private AspectOrderProperties aspectOrder = new AspectOrderProperties();

  @Data
  public static class IdempotentProperties {

    /** Enables idempotent auto-configuration. */
    private boolean enabled = true;

    /** Backend implementation used by idempotent handler selection. */
    private ConcurrencyBackend backend = ConcurrencyBackend.AUTO;

    /** Prefix used by idempotent related keys. */
    private String prefix = "idempotent:";
  }

  @Data
  public static class RateLimitProperties {

    /** Enables rate-limit auto-configuration. */
    private boolean enabled = true;

    /** Backend implementation used by rate-limit handler selection. */
    private ConcurrencyBackend backend = ConcurrencyBackend.AUTO;

    /** Prefix used by rate-limit related keys. */
    private String prefix = "rateLimit:";
  }

  @Data
  public static class LockProperties {

    /** Enables lock auto-configuration. */
    private boolean enabled = true;

    /** Backend implementation used by lock handler selection. */
    private ConcurrencyBackend backend = ConcurrencyBackend.AUTO;

    /** Prefix used by lock related keys. */
    private String prefix = "lock:";

    /** 简单 Redis 锁的固定 TTL，单位为秒；到期自动释放，不续期。 */
    private long redisTtlSeconds = 60;
  }

  @Data
  public static class RedisProperties {

    /** Enables Redis helper auto-configuration. */
    private boolean enabled = true;
  }

  @Data
  public static class CacheProperties {

    /** Enables cache auto-configuration. */
    private boolean enabled = true;

    /** Static prefix added to cache keys. */
    private String prefix = "";

    /** Separator used when building cache key prefixes. */
    private String separator = ":";

    /** Default cache TTL. */
    private Duration defaultTtl = Duration.ofMinutes(5);

    /**
     * Whether to cache null values. When enabled, null results are cached to prevent cache
     * penetration. Default is true.
     */
    private boolean nullCachingEnabled = true;

    /**
     * Percentage of jitter applied to TTL values to prevent cache stampede. For example, a value of
     * 10 means each entry's TTL is randomly shifted by up to ±10% when it is written, independently
     * per key. Set to 0 to disable jitter. Default is 0.
     */
    private int ttlJitterPercentage;

    public void setTtlJitterPercentage(int ttlJitterPercentage) {
      if (ttlJitterPercentage < 0 || ttlJitterPercentage > 100) {
        throw new IllegalArgumentException(
            "Cache TTL jitter percentage must be between 0 and 100.");
      }
      this.ttlJitterPercentage = ttlJitterPercentage;
    }

    /** Per-cache TTL overrides. */
    private Map<String, Duration> ttl = new LinkedHashMap<>();
  }

  @Data
  public static class ExcelProperties {

    /** Fine-grained converter switches shared by EasyExcel, FastExcel and Fesod. */
    private ConverterProperties converters = new ConverterProperties();

    @Data
    public static class ConverterProperties {

      /** Enables automatic registration of built-in Excel converters. */
      private boolean enabled = true;

      /** Enables the Boolean Excel converter. */
      private boolean booleanEnabled = true;

      /** Enables the Long Excel converter. */
      private boolean longEnabled = true;

      /** Enables the Float Excel converter. */
      private boolean floatEnabled = true;

      /** Enables the Double Excel converter. */
      private boolean doubleEnabled = true;

      /** Enables the BigInteger Excel converter. */
      private boolean bigIntegerEnabled = true;

      /** Enables the BigDecimal Excel converter. */
      private boolean bigDecimalEnabled = true;

      /** Enables the java.util.Date Excel converter. */
      private boolean dateEnabled = true;

      /** Enables the LocalDateTime Excel converter. */
      private boolean localDateTimeEnabled = true;

      /** Enables the LocalDate Excel converter. */
      private boolean localDateEnabled = true;

      /** Enables the LocalTime Excel converter. */
      private boolean localTimeEnabled = true;
    }
  }

  @Data
  public static class JacksonProperties {

    /** Enables Jackson auto-configuration. */
    private boolean enabled = true;

    /** Enables date-time related Jackson customization. */
    private boolean dateTimeEnabled = true;

    /**
     * Serializes long values as strings on write, to avoid precision loss on the front-end
     * (JavaScript Number cannot safely represent integers beyond 2^53). Only affects serialization
     * (write); deserialization accepts both numbers and strings.
     */
    private boolean serializeLongAsString = true;

    /** Serializes BigDecimal values as strings on write. */
    private boolean serializeBigDecimalAsString = true;

    /** Removes trailing zeros before serializing BigDecimal values. */
    private boolean bigDecimalStripTrailingZeros = false;

    /** Serializes float and double values as strings on write. */
    private boolean serializeFloatingAsString = false;

    /** Adds enum description fields during serialization. */
    private boolean enumDescEnabled = true;

    /** Default suffix used by derived enum description fields. */
    private String enumNameSuffix = "name";

    /** Candidate enum code-to-name field pairs, matched in declaration order. */
    private Map<String, String> enumMappings = defaultEnumMappings();

    private static Map<String, String> defaultEnumMappings() {
      Map<String, String> mappings = new LinkedHashMap<>();
      mappings.put("code", "name");
      mappings.put("key", "value");
      return mappings;
    }
  }

  @Data
  public static class LogProperties {
    /** 是否启用调用日志和相关自动配置。 */
    private boolean enabled = true;

    /** 旧版 trace 配置入口；运行时独立绑定 velo.trace，新配置逐项优先于这里的值。 */
    private TraceProperties trace = new TraceProperties();

    /** 全局默认配置；来源可覆盖，注解优先。 */
    private InvocationDefaults defaults = new InvocationDefaults();

    private InvocationSources sources = new InvocationSources();
  }

  @Data
  public static class LogFeatureProperties {
    /** 未配置时继承全局或内置默认值。 */
    private Boolean enabled;

    /** 未配置时继承全局或内置默认级别。 */
    private LogLevel level;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class SlowLogProperties extends LogFeatureProperties {
    /** 慢调用阈值，单位毫秒；0 表示记录全部耗时。 */
    private Long thresholdMs;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class HeaderCaptureProperties extends LogFeatureProperties {
    /** 允许采集的头名称；空列表表示全部头。 */
    private List<String> allowlist;
  }

  @Data
  public static class InvocationDefaults {
    public static final int DEFAULT_MAX_PAYLOAD_LENGTH = 4096;

    /** 默认 4096 字符；-1 不限字符数，0 不采集对象载荷，正数限制单行载荷长度。 */
    private int maxPayloadLength = DEFAULT_MAX_PAYLOAD_LENGTH;

    private LogFeatureProperties entryArgs = new LogFeatureProperties();
    private LogFeatureProperties exitArgs = new LogFeatureProperties();
    private LogFeatureProperties exitResult = new LogFeatureProperties();
    private SlowLogProperties slowLog = defaultSlowLog();
    private HeaderCaptureProperties requestHeaders = new HeaderCaptureProperties();
    private HeaderCaptureProperties responseHeaders = new HeaderCaptureProperties();
    private LogFeatureProperties errorLog = new LogFeatureProperties();

    private static SlowLogProperties defaultSlowLog() {
      SlowLogProperties value = new SlowLogProperties();
      value.setEnabled(true);
      value.setLevel(LogLevel.WARN);
      value.setThresholdMs(1000L);
      return value;
    }
  }

  @Data
  public static class InvocationSources {
    private InvocationSourceProperties controller = new InvocationSourceProperties();
    private InvocationSourceProperties feign = new InvocationSourceProperties();
    private InvocationSourceProperties invoke = new InvocationSourceProperties();
    private InvocationSourceProperties xxlJob = new InvocationSourceProperties();
    private InvocationSourceProperties scheduled = new InvocationSourceProperties();
  }

  @Data
  public static class TraceProperties {

    /** 是否自动生成和传播 traceId。 */
    private boolean enabled = true;

    /** 日志框架读取 traceId 的 MDC 键。 */
    private String mdcKey = "traceId";

    /** 是否将当前 traceId 传播到 Feign 请求。 */
    private boolean feignPropagationEnabled = true;

    /** 用户未配置日志格式时，在 Spring Boot 默认级别格式中添加 traceId。 */
    private boolean loggingPatternEnabled = true;
  }

  @Data
  public static class InvocationSourceProperties {
    /** 是否启用来源日志；关闭后注解不能重新开启。 */
    private boolean enabled = true;

    private LogFeatureProperties entryArgs = new LogFeatureProperties();
    private LogFeatureProperties exitArgs = new LogFeatureProperties();
    private LogFeatureProperties exitResult = new LogFeatureProperties();
    private SlowLogProperties slowLog = new SlowLogProperties();
    private HeaderCaptureProperties requestHeaders = new HeaderCaptureProperties();
    private HeaderCaptureProperties responseHeaders = new HeaderCaptureProperties();
    private LogFeatureProperties errorLog = new LogFeatureProperties();
  }

  @Data
  public static class WebProperties {

    /** 是否启用 Web MVC 自动配置。 */
    private boolean enabled = true;

    /** CORS settings. */
    private CorsProperties cors = new CorsProperties();
  }

  @Data
  public static class CorsProperties {

    /** 是否启用 Web MVC 跨域配置。 */
    private boolean enabled;

    /**
     * Comma-separated or array-style list of allowed origin patterns. Defaults to {@code *} (all
     * origins).
     */
    private String[] allowedOriginPatterns = {"*"};

    /** Comma-separated or array-style list of allowed HTTP methods. */
    private String[] allowedMethods = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};

    /**
     * Whether browsers may include credentials such as cookies or HTTP authentication in
     * cross-origin requests. Defaults to {@code false}; enable it only when cross-origin
     * credentials are required.
     */
    private boolean allowCredentials;

    /** Max age of preflight cache in seconds. */
    private long maxAge = 3600;
  }

  @Data
  public static class FeignProperties {

    /** Enables Feign client logging auto-configuration. */
    private boolean enabled = true;
  }

  @Data
  public static class XssProperties {

    /** Default XSS cleaning strategy. {@code NONE} disables the built-in cleaner. */
    private XssStrategy strategy = XssStrategy.NONE;

    /**
     * 是否启用 Web MVC 字符串转换目标。 A user-provided {@code XssCleaner} can still be used when the built-in
     * strategy is {@code NONE}.
     */
    private boolean webEnabled = true;

    /**
     * Enables XSS cleaning for ordinary Jackson String properties. This is disabled by default
     * because Jackson customization is global to the mapper.
     */
    private boolean jacksonEnabled;
  }

  @Data
  public static class DateTimeFormatProperties {
    /** Default time pattern. */
    private String time = "HH:mm:ss";

    /** Default date pattern. */
    private String date = "yyyy-MM-dd";

    /** Default date-time pattern. */
    private String dateTime = "yyyy-MM-dd HH:mm:ss";

    /** Default time zone used by date based converters and serializers. */
    private String timeZone = "GMT+8";
  }

  @Data
  public static class SpringConverterProperties {

    /** Enables automatic registration of built-in date-time converters. */
    private boolean dateTimeEnabled = true;
  }

  @Data
  public static class MybatisPlusProperties {

    /** Enables MyBatis-Plus auto-configuration. */
    private boolean enabled = true;

    /** 启用分页内部拦截器 Bean；MyBatis-Plus 3.5.9 及以上还需要引入对应的 JSQLParser 扩展模块。 */
    private boolean paginationEnabled = true;

    /** Enables the optimistic locker inner interceptor bean. */
    private boolean optimisticLockerEnabled = true;

    /** 启用防全表更新与删除内部拦截器 Bean；MyBatis-Plus 3.5.9 及以上还需要引入对应的 JSQLParser 扩展模块。 */
    private boolean blockAttackEnabled = true;
  }

  @Data
  public static class AspectOrderProperties {

    /** Order for the idempotent aspect. */
    private int idempotent = VeloAdvisorOrder.CONCURRENCY_IDEMPOTENT;

    /** Order for the rate-limit aspect. */
    private int rateLimit = VeloAdvisorOrder.CONCURRENCY_RATE_LIMIT;

    /** Order for the lock aspect. */
    private int lock = VeloAdvisorOrder.CONCURRENCY_LOCK;

    /** Order for the invoke-log aspect. */
    private int invokeLog = VeloAdvisorOrder.LOG_INVOKE;

    /** Order for the controller-log aspect. */
    private int controllerLog = VeloAdvisorOrder.LOG_CONTROLLER;

    /** Order for the feign-log aspect. */
    private int feignLog = VeloAdvisorOrder.LOG_FEIGN;
  }

  @Data
  public static class BannerProperties {

    /**
     * Prints the Velo startup banner with a summary of enabled features. Disabled by default;
     * enable to print the banner on startup.
     */
    private boolean enabled = false;
  }
}
