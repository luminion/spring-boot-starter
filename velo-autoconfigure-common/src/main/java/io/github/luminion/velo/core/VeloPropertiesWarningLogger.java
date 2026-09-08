package io.github.luminion.velo.core;

import io.github.luminion.velo.VeloProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 在应用启动时检查常见配置问题并输出告警。
 *
 * <p>这里只做提示，不修改配置值、不改变现有回退逻辑，也不阻止应用启动。</p>
 *
 * @author luminion
 * @since 1.3.1
 */
public class VeloPropertiesWarningLogger implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(VeloPropertiesWarningLogger.class);

    private final VeloProperties properties;

    public VeloPropertiesWarningLogger(VeloProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        for (String warning : collectWarnings(properties)) {
            log.warn("[Velo Starter] 配置告警：{}", warning);
        }
    }

    @SuppressWarnings("deprecation")
    static List<String> collectWarnings(VeloProperties properties) {
        List<String> warnings = new ArrayList<>();
        if (properties == null) {
            return warnings;
        }

        VeloProperties.IdempotentProperties idempotent = properties.getIdempotent();
        if (idempotent == null) {
            warnings.add("velo.idempotent 为空，相关功能可能在运行时失败。当前仅告警。");
        } else {
            warnBlankIfEnabled(warnings, "velo.idempotent.prefix", idempotent.isEnabled(),
                    idempotent.getPrefix(), "幂等键可能与其他业务键发生冲突");
        }

        VeloProperties.RateLimitProperties rateLimit = properties.getRateLimit();
        if (rateLimit == null) {
            warnings.add("velo.rate-limit 为空，相关功能可能在运行时失败。当前仅告警。");
        } else {
            warnBlankIfEnabled(warnings, "velo.rate-limit.prefix", rateLimit.isEnabled(),
                    rateLimit.getPrefix(), "限流键可能与其他业务键发生冲突");
        }

        VeloProperties.LockProperties lock = properties.getLock();
        if (lock == null) {
            warnings.add("velo.lock 为空，相关功能可能在运行时失败。当前仅告警。");
        } else if (lock.isEnabled()) {
            warnBlank(warnings, "velo.lock.prefix", lock.getPrefix(), "锁键可能与其他业务键发生冲突");
            warnPositiveDuration(warnings, "velo.lock.retry-interval", lock.getRetryInterval(),
                    "Redis 锁重试间隔必须大于 0");
        }

        VeloProperties.CacheProperties cache = properties.getCache();
        if (cache == null) {
            warnings.add("velo.cache 为空，缓存自动配置可能在运行时失败。当前仅告警。");
        } else if (cache.isEnabled()) {
            warnPositiveDuration(warnings, "velo.cache.default-ttl", cache.getDefaultTtl(),
                    "缓存默认 TTL 必须大于 0");
            warnBlank(warnings, "velo.cache.separator", cache.getSeparator(),
                    "缓存前缀分隔符为空时会回退为 :");
            Map<String, Duration> ttl = cache.getTtl();
            if (ttl == null) {
                warnings.add("velo.cache.ttl 为空，单项缓存 TTL 配置可能在运行时失败。当前仅告警。");
            } else {
                for (Map.Entry<String, Duration> entry : ttl.entrySet()) {
                    String property = "velo.cache.ttl[" + String.valueOf(entry.getKey()) + "]";
                    if (!StringUtils.hasText(entry.getKey())) {
                        warnings.add(property + " 的缓存名称为空，可能无法按预期匹配缓存。当前仅告警。");
                    }
                    warnPositiveDuration(warnings, property, entry.getValue(),
                            "单项缓存 TTL 必须大于 0");
                }
            }
        }

        VeloProperties.LogProperties logProperties = properties.getLog();
        if (logProperties == null) {
            warnings.add("velo.log 为空，日志自动配置可能在运行时失败。当前仅告警。");
        } else if (logProperties.isEnabled()) {
            VeloProperties.TraceProperties trace = logProperties.getTrace();
            if (trace == null) {
                warnings.add("velo.log.trace 为空，链路追踪相关功能可能在运行时失败。当前仅告警。");
            } else if (trace.isEnabled()) {
                warnBlank(warnings, "velo.log.trace.header-name", trace.getHeaderName(),
                        "请求头名称为空时链路 ID 无法正常传递");
                warnBlank(warnings, "velo.log.trace.mdc-key", trace.getMdcKey(),
                        "MDC 键为空时链路 ID 无法正常写入日志上下文");
            }

            VeloProperties.InvocationProperties invocation = logProperties.getInvocation();
            if (invocation == null) {
                warnings.add("velo.log.invocation 为空，调用日志相关功能可能在运行时失败。当前仅告警。");
            } else if (invocation.isEnabled() && invocation.getMaxPayloadLength() < -1) {
                warnings.add("velo.log.invocation.max-payload-length 当前值为 "
                        + invocation.getMaxPayloadLength()
                        + "，仅 -1 或大于等于 0 有明确语义。当前仅告警。");
            }
        }

        VeloProperties.JacksonProperties jackson = properties.getJackson();
        if (jackson == null) {
            warnings.add("velo.jackson 为空，Jackson 自动配置可能在运行时失败。当前仅告警。");
        } else if (jackson.isEnabled() && jackson.isEnumDescEnabled()) {
            warnBlank(warnings, "velo.jackson.enum-name-suffix", jackson.getEnumNameSuffix(),
                    "枚举描述字段后缀为空时可能覆盖或生成歧义字段");
            Map<String, String> enumMappings = jackson.getEnumMappings();
            if (enumMappings != null) {
                for (Map.Entry<String, String> entry : enumMappings.entrySet()) {
                    if (!StringUtils.hasText(entry.getKey()) || !StringUtils.hasText(entry.getValue())) {
                        warnings.add("velo.jackson.enum-mappings 包含空的代码字段或名称字段，该映射会被跳过。当前仅告警。");
                    }
                }
            }
        }

        VeloProperties.WebProperties web = properties.getWeb();
        if (web == null) {
            warnings.add("velo.web 为空，Web 自动配置可能在运行时失败。当前仅告警。");
        } else {
            VeloProperties.CorsProperties cors = web.getCors();
            if (cors == null) {
                warnings.add("velo.web.cors 为空，CORS 配置可能在运行时失败。当前仅告警。");
            } else if (web.isAllowCors() || cors.isEnabled()) {
                warnNonEmptyArray(warnings, "velo.web.cors.allowed-origin-patterns",
                        cors.getAllowedOriginPatterns(), "CORS 未配置允许的 Origin");
                warnNonEmptyArray(warnings, "velo.web.cors.allowed-methods",
                        cors.getAllowedMethods(), "CORS 未配置允许的 HTTP 方法");
                if (cors.getMaxAge() < 0) {
                    warnings.add("velo.web.cors.max-age 当前值为 " + cors.getMaxAge()
                            + "，可能导致预检缓存行为不符合预期。当前仅告警。");
                }
            }
        }

        VeloProperties.SpringConverterProperties springConverter = properties.getSpringConverter();
        if (springConverter == null) {
            warnings.add("velo.spring-converter 为空，日期时间转换配置可能在运行时失败。当前仅告警。");
        }
        VeloProperties.ExcelProperties excel = properties.getExcel();
        if (excel == null) {
            warnings.add("velo.excel 为空，Excel 自动配置可能在运行时失败。当前仅告警。");
        } else if (excel.isEnabled() && excel.getConverters() == null) {
            warnings.add("velo.excel.converters 为空，Excel 转换器注册可能在运行时失败。当前仅告警。");
        }

        boolean dateTimeFeatureEnabled = springConverter != null && springConverter.isDateTimeEnabled();
        dateTimeFeatureEnabled = dateTimeFeatureEnabled
                || jackson != null && jackson.isEnabled() && jackson.isDateTimeEnabled();
        dateTimeFeatureEnabled = dateTimeFeatureEnabled
                || excel != null && excel.isEnabled() && excel.getConverters() != null
                && excel.getConverters().isEnabled();
        if (dateTimeFeatureEnabled) {
            VeloProperties.DateTimeFormatProperties dateTimeFormat = properties.getDateTimeFormat();
            if (dateTimeFormat == null) {
                warnings.add("velo.date-time-format 为空，日期时间转换可能在运行时失败。当前仅告警。");
            } else {
                warnDateTimePattern(warnings, "velo.date-time-format.date", dateTimeFormat.getDate());
                warnDateTimePattern(warnings, "velo.date-time-format.time", dateTimeFormat.getTime());
                warnDateTimePattern(warnings, "velo.date-time-format.date-time", dateTimeFormat.getDateTime());
                warnTimeZone(warnings, dateTimeFormat.getTimeZone());
            }
        }
        return warnings;
    }

    private static void warnBlankIfEnabled(List<String> warnings, String property, boolean enabled,
            String value, String consequence) {
        if (enabled) {
            warnBlank(warnings, property, value, consequence);
        }
    }

    private static void warnBlank(List<String> warnings, String property, String value, String consequence) {
        if (!StringUtils.hasText(value)) {
            warnings.add(property + " 为空或仅包含空白，" + consequence + "。当前仅告警。");
        }
    }

    private static void warnPositiveDuration(List<String> warnings, String property, Duration value,
            String description) {
        if (value == null || value.isZero() || value.isNegative()) {
            warnings.add(property + " 当前值为 " + String.valueOf(value) + "，" + description + "。当前仅告警。");
        }
    }

    private static void warnNonEmptyArray(List<String> warnings, String property, String[] values,
            String consequence) {
        if (values == null || values.length == 0) {
            warnings.add(property + " 为空，" + consequence + "。当前仅告警。");
            return;
        }
        for (int i = 0; i < values.length; i++) {
            if (!StringUtils.hasText(values[i])) {
                warnings.add(property + "[" + i + "] 为空或仅包含空白，" + consequence + "。当前仅告警。");
            }
        }
    }

    private static void warnDateTimePattern(List<String> warnings, String property, String pattern) {
        if (!StringUtils.hasText(pattern)) {
            warnings.add(property + " 为空或仅包含空白，日期时间转换可能失败。当前仅告警。");
            return;
        }
        try {
            DateTimeFormatter.ofPattern(pattern);
        } catch (IllegalArgumentException ex) {
            warnings.add(property + " 不是有效的 DateTimeFormatter 模式，日期时间转换可能失败。当前仅告警。");
        }
    }

    private static void warnTimeZone(List<String> warnings, String timeZone) {
        if (!StringUtils.hasText(timeZone)) {
            warnings.add("velo.date-time-format.time-zone 为空，日期时间转换可能失败。当前仅告警。");
            return;
        }
        try {
            ZoneId.of(timeZone);
        } catch (DateTimeException ex) {
            warnings.add("velo.date-time-format.time-zone 当前值不是有效的 ZoneId，部分日期转换可能失败或回退为 GMT。当前仅告警。");
        }
    }
}
