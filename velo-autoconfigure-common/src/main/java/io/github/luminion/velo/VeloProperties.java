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

/**
 * Velo 的统一配置属性。
 */
@Data
@ConfigurationProperties("velo")
public class VeloProperties {

    /**
     * 是否启用开箱即用的默认增强；设置为 {@code false} 时使用无侵入模式。
     */
    private boolean opinionated = true;

    /**
     * 启动横幅配置。
     */
    private BannerProperties banner = new BannerProperties();

    /**
     * Web、Jackson 和 Excel 功能共用的日期时间格式配置。
     */
    private DateTimeFormatProperties dateTimeFormat = new DateTimeFormatProperties();

    /**
     * Spring 类型转换器配置。
     */
    private SpringConverterProperties springConverter = new SpringConverterProperties();

    /**
     * 幂等功能配置。
     */
    private IdempotentProperties idempotent = new IdempotentProperties();

    /**
     * 限流功能配置。
     */
    private RateLimitProperties rateLimit = new RateLimitProperties();

    /**
     * 锁功能配置。
     */
    private LockProperties lock = new LockProperties();

    /**
     * Redis 辅助功能配置。
     */
    private RedisProperties redis = new RedisProperties();

    /**
     * 缓存功能配置。
     */
    private CacheProperties cache = new CacheProperties();

    /**
     * Excel 集成配置。
     */
    private ExcelProperties excel = new ExcelProperties();

    /**
     * Jackson 集成配置。
     */
    private JacksonProperties jackson = new JacksonProperties();

    /**
     * XSS 清洗策略与适配目标设置。
     */
    private XssProperties xss = new XssProperties();

    /**
     * 日志自动配置。
     */
    private LogProperties log = new LogProperties();

    /**
     * MyBatis-Plus 集成配置。
     */
    private MybatisPlusProperties mybatisPlus = new MybatisPlusProperties();

    /**
     * Web 相关配置。
     */
    private WebProperties web = new WebProperties();

    /**
     * Feign 相关配置。
     */
    private FeignProperties feign = new FeignProperties();

    /**
     * 切面执行顺序配置。
     */
    private AspectOrderProperties aspectOrder = new AspectOrderProperties();

    @Data
    public static class IdempotentProperties {

        /**
         * 是否启用幂等自动配置。
         */
        private boolean enabled = true;

        /**
         * 幂等处理器选用的后端实现。
         */
        private ConcurrencyBackend backend = ConcurrencyBackend.AUTO;

        /**
         * 幂等相关键的前缀。
         */
        private String prefix = "idempotent:";
    }

    @Data
    public static class RateLimitProperties {

        /**
         * 是否启用限流自动配置。
         */
        private boolean enabled = true;

        /**
         * 限流处理器选用的后端实现。
         */
        private ConcurrencyBackend backend = ConcurrencyBackend.AUTO;

        /**
         * 限流相关键的前缀。
         */
        private String prefix = "rateLimit:";
    }

    @Data
    public static class LockProperties {

        /**
         * 是否启用锁自动配置。
         */
        private boolean enabled = true;

        /**
         * 锁处理器选用的后端实现。
         */
        private ConcurrencyBackend backend = ConcurrencyBackend.AUTO;

        /**
         * 锁相关键的前缀。
         */
        private String prefix = "lock:";

        /**
         * 简单 Redis 锁的固定 TTL，单位为秒；到期自动释放，不续期。
         */
        private long redisTtlSeconds = 60;
    }

    @Data
    public static class RedisProperties {

        /**
         * 是否启用 Redis 辅助功能自动配置。
         */
        private boolean enabled = true;
    }

    @Data
    public static class CacheProperties {

        /**
         * 是否启用缓存自动配置。
         */
        private boolean enabled = true;

        /**
         * 构建缓存键前缀时使用的分隔符。
         */
        private String separator = ":";

        /**
         * 是否启用缓存事务感知；默认关闭，开启后相关缓存操作按 Spring 原生规则延迟到事务提交后执行。
         */
        private boolean transactionAware;

        /**
         * 缓存有效期的随机抖动百分比，用于避免缓存集中失效。例如设置为 10 时，
         * 每个键在写入时独立计算有效期，随机调整幅度不超过 ±10%。设置为 0 时关闭抖动，默认值为 0。
         */
        private int ttlJitterPercentage;

        public void setTtlJitterPercentage(int ttlJitterPercentage) {
            if (ttlJitterPercentage < 0 || ttlJitterPercentage > 100) {
                throw new IllegalArgumentException(
                        "Cache TTL jitter percentage must be between 0 and 100.");
            }
            this.ttlJitterPercentage = ttlJitterPercentage;
        }

        /**
         * 按缓存名称覆盖默认有效期；0 表示不过期，正值至少为 1ms。
         */
        private Map<String, Duration> ttl = new LinkedHashMap<>();
    }

    @Data
    public static class ExcelProperties {

        /**
         * EasyExcel、FastExcel 和 Fesod 共用的细粒度转换器开关。
         */
        private ConverterProperties converters = new ConverterProperties();

        @Data
        public static class ConverterProperties {

            /**
             * 是否自动注册内置 Excel 转换器。
             */
            private boolean enabled = true;

            /**
             * 是否启用 Boolean 类型的 Excel 转换器。
             */
            private boolean booleanEnabled = true;

            /**
             * 是否启用 Long 类型的 Excel 转换器。
             */
            private boolean longEnabled = true;

            /**
             * 是否启用 Float 类型的 Excel 转换器。
             */
            private boolean floatEnabled = true;

            /**
             * 是否启用 Double 类型的 Excel 转换器。
             */
            private boolean doubleEnabled = true;

            /**
             * 是否启用 BigInteger 类型的 Excel 转换器。
             */
            private boolean bigIntegerEnabled = true;

            /**
             * 是否启用 BigDecimal 类型的 Excel 转换器。
             */
            private boolean bigDecimalEnabled = true;

            /**
             * 是否启用 java.util.Date 类型的 Excel 转换器。
             */
            private boolean dateEnabled = true;

            /**
             * 是否启用 LocalDateTime 类型的 Excel 转换器。
             */
            private boolean localDateTimeEnabled = true;

            /**
             * 是否启用 LocalDate 类型的 Excel 转换器。
             */
            private boolean localDateEnabled = true;

            /**
             * 是否启用 LocalTime 类型的 Excel 转换器。
             */
            private boolean localTimeEnabled = true;
        }
    }

    @Data
    public static class JacksonProperties {

        /**
         * 是否启用 Jackson 自动配置。
         */
        private boolean enabled = true;

        /**
         * 是否启用 Jackson 日期时间格式增强。
         */
        private boolean dateTimeEnabled = true;

        /**
         * 序列化时将 long 值输出为字符串，避免前端精度丢失
         * （JavaScript 的 Number 无法安全表示超过 2^53 的整数）。
         * 仅影响序列化；反序列化同时接受数字和字符串。
         */
        private boolean serializeLongAsString = true;

        /**
         * 是否在序列化时将 BigDecimal 值输出为字符串。
         */
        private boolean serializeBigDecimalAsString = true;

        /**
         * 是否在序列化 BigDecimal 值前移除尾零。
         */
        private boolean bigDecimalStripTrailingZeros = false;

        /**
         * 是否在序列化时将 float 和 double 值输出为字符串。
         */
        private boolean serializeFloatingAsString = false;

        /**
         * 是否在序列化时添加枚举描述字段。
         */
        private boolean enumDescEnabled = true;

        /**
         * 派生枚举描述字段的默认后缀。
         */
        private String enumNameSuffix = "name";

        /**
         * 枚举编码字段与名称字段的候选映射，按声明顺序匹配。
         */
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
        /**
         * 是否启用调用日志和相关自动配置。
         */
        private boolean enabled = true;

        /**
         * 旧版 trace 配置入口；运行时独立绑定 velo.trace，新配置逐项优先于这里的值。
         */
        private TraceProperties trace = new TraceProperties();

        /**
         * 全局默认配置；来源可覆盖，注解优先。
         */
        private InvocationDefaults defaults = new InvocationDefaults();

        private InvocationSources sources = new InvocationSources();
    }

    @Data
    public static class LogFeatureProperties {
        /**
         * 未配置时继承全局或内置默认值。
         */
        private Boolean enabled;

        /**
         * 未配置时继承全局或内置默认级别。
         */
        private LogLevel level;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SlowLogProperties extends LogFeatureProperties {
        /**
         * 慢调用阈值，单位毫秒；0 表示记录全部耗时。
         */
        private Long thresholdMs;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class HeaderCaptureProperties extends LogFeatureProperties {
        /**
         * 允许采集的头名称；空列表表示全部头。
         */
        private List<String> allowlist;
    }

    @Data
    public static class InvocationDefaults {

        /**
         * 默认 -1 完整输出，0 关闭对象载荷；不支持字符截断，其他值会在日志引擎启动时报错。
         */
        private int maxPayloadLength = -1;

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

        /**
         * 是否自动生成和传播 traceId。
         */
        private boolean enabled = true;

        /**
         * 日志框架读取 traceId 的 MDC 键。
         */
        private String mdcKey = "traceId";

        /**
         * 是否将当前 traceId 传播到 Feign 请求。
         */
        private boolean feignPropagationEnabled = true;

        /**
         * 用户未配置日志格式时，在 Spring Boot 默认级别格式中添加 traceId。
         */
        private boolean loggingPatternEnabled = true;
    }

    @Data
    public static class InvocationSourceProperties {
        /**
         * 是否启用来源日志；关闭后注解不能重新开启。
         */
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

        /**
         * 是否启用 Web MVC 自动配置。
         */
        private boolean enabled = true;

        /**
         * 跨域配置。
         */
        private CorsProperties cors = new CorsProperties();
    }

    @Data
    public static class CorsProperties {

        /**
         * 是否启用 Web MVC 跨域配置。
         */
        private boolean enabled;

        /**
         * 允许的来源匹配模式列表，支持逗号分隔或数组形式。
         * 默认值为 {@code *}，表示允许所有来源。
         */
        private String[] allowedOriginPatterns = {"*"};

        /**
         * 允许的 HTTP 方法列表，支持逗号分隔或数组形式。
         */
        private String[] allowedMethods = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};

        /**
         * 是否允许浏览器在跨域请求中携带 Cookie 或 HTTP 身份认证等凭据。
         * 默认为 {@code false}，仅在需要跨域凭据时启用。
         */
        private boolean allowCredentials;

        /**
         * 预检请求的缓存有效期，单位为秒。
         */
        private long maxAge = 3600;
    }

    @Data
    public static class FeignProperties {

        /**
         * 是否启用 Feign 客户端日志自动配置。
         */
        private boolean enabled = true;
    }

    @Data
    public static class XssProperties {

        /**
         * 默认 XSS 清洗策略；设置为 {@code NONE} 时禁用内置清洗器。
         */
        private XssStrategy strategy = XssStrategy.NONE;

        /**
         * 是否启用 Web MVC 字符串转换目标。内置策略为 {@code NONE} 时，
         * 仍可使用用户提供的 {@code XssCleaner}。
         */
        private boolean webEnabled = true;

        /**
         * 是否对 Jackson 的普通 String 属性启用 XSS 清洗。
         * 由于 Jackson 配置会影响整个映射器，默认关闭此功能。
         */
        private boolean jacksonEnabled;
    }

    @Data
    public static class DateTimeFormatProperties {
        /**
         * 默认时间格式。
         */
        private String time = "HH:mm:ss";

        /**
         * 默认日期格式。
         */
        private String date = "yyyy-MM-dd";

        /**
         * 默认日期时间格式。
         */
        private String dateTime = "yyyy-MM-dd HH:mm:ss";

        /**
         * 日期转换器和序列化器使用的默认时区。
         */
        private String timeZone = "GMT+8";
    }

    @Data
    public static class SpringConverterProperties {

        /**
         * 是否自动注册内置日期时间转换器。
         */
        private boolean dateTimeEnabled = true;
    }

    @Data
    public static class MybatisPlusProperties {

        /**
         * 是否启用 MyBatis-Plus 自动配置。
         */
        private boolean enabled = true;

        /**
         * 启用分页内部拦截器 Bean；MyBatis-Plus 3.5.9 及以上还需要引入对应的 JSQLParser 扩展模块。
         */
        private boolean paginationEnabled = true;

        /**
         * 是否启用乐观锁内部拦截器 Bean。
         */
        private boolean optimisticLockerEnabled = true;

        /**
         * 启用防全表更新与删除内部拦截器 Bean；MyBatis-Plus 3.5.9 及以上还需要引入对应的 JSQLParser 扩展模块。
         */
        private boolean blockAttackEnabled = true;
    }

    @Data
    public static class AspectOrderProperties {

        /**
         * 幂等切面的执行顺序。
         */
        private int idempotent = VeloAdvisorOrder.CONCURRENCY_IDEMPOTENT;

        /**
         * 限流切面的执行顺序。
         */
        private int rateLimit = VeloAdvisorOrder.CONCURRENCY_RATE_LIMIT;

        /**
         * 锁切面的执行顺序。
         */
        private int lock = VeloAdvisorOrder.CONCURRENCY_LOCK;

        /**
         * 方法调用日志切面的执行顺序。
         */
        private int invokeLog = VeloAdvisorOrder.LOG_INVOKE;

        /**
         * 控制器日志切面的执行顺序。
         */
        private int controllerLog = VeloAdvisorOrder.LOG_CONTROLLER;

        /**
         * Feign 日志切面的执行顺序。
         */
        private int feignLog = VeloAdvisorOrder.LOG_FEIGN;
    }

    @Data
    public static class BannerProperties {

        /**
         * 是否在启动时打印 Velo 横幅及已启用功能的摘要。默认关闭，启用后在启动时输出。
         */
        private boolean enabled = false;
    }
}
