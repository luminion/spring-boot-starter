package io.github.luminion.velo.core;

import io.github.luminion.velo.ConcurrencyBackend;
import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.idempotent.IdempotentHandler;
import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.ratelimit.RateLimitHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * 在 {@code velo.banner.enabled=true} 时打印 Velo 启动横幅与各功能开关概览。
 * <p>
 * 与 Spring Boot 的 banner 一致，直接输出到控制台（{@code System.out}），
 * 不经过日志框架，因此不进入日志文件、也不受日志级别控制。
 * <p>
 * 默认关闭，可通过 {@code velo.banner.enabled=true} 开启。
 *
 * @author luminion
 */
public class VeloBannerPrinter implements SmartInitializingSingleton {

    private static final String MISSING_CONFIGURATION = "unavailable (config missing)";

    private final VeloProperties properties;
    private final ObjectProvider<IdempotentHandler> idempotentHandler;
    private final ObjectProvider<RateLimitHandler> rateLimitHandler;
    private final ObjectProvider<LockHandler> lockHandler;

    public VeloBannerPrinter(VeloProperties properties,
            ObjectProvider<IdempotentHandler> idempotentHandler,
            ObjectProvider<RateLimitHandler> rateLimitHandler,
            ObjectProvider<LockHandler> lockHandler) {
        this.properties = properties;
        this.idempotentHandler = idempotentHandler;
        this.rateLimitHandler = rateLimitHandler;
        this.lockHandler = lockHandler;
    }

    @Override
    public void afterSingletonsInstantiated() {
        VeloProperties.BannerProperties banner = properties == null ? null : properties.getBanner();
        if (banner == null || !banner.isEnabled()) {
            return;
        }
        System.out.print(buildBanner());
        System.out.flush();
    }

    private String buildBanner() {
        StringBuilder sb = new StringBuilder();
        sb.append('\n');
        sb.append("__     __   _       \n");
        sb.append("\\ \\   / /__| | ___    Velo Spring Boot Starter\n");
        sb.append(" \\ \\ / / _ \\ |/ _ \\   opinionated=").append(properties.isOpinionated()).append('\n');
        sb.append("  \\ V /  __/ | (_) |  \n");
        sb.append("   \\_/ \\___|_|\\___/   \n");
        VeloProperties.IdempotentProperties idempotent = properties.getIdempotent();
        line(sb, "idempotent", idempotent == null ? MISSING_CONFIGURATION : concurrency(idempotent.isEnabled(),
                idempotent.getBackend(), idempotentHandler));
        VeloProperties.RateLimitProperties rateLimit = properties.getRateLimit();
        line(sb, "rate-limit", rateLimit == null ? MISSING_CONFIGURATION : concurrency(rateLimit.isEnabled(),
                rateLimit.getBackend(), rateLimitHandler));
        VeloProperties.LockProperties lock = properties.getLock();
        line(sb, "lock", lock == null ? MISSING_CONFIGURATION : concurrency(lock.isEnabled(),
                lock.getBackend(), lockHandler));
        VeloProperties.CacheProperties cache = properties.getCache();
        line(sb, "cache", cache == null ? MISSING_CONFIGURATION : cache.isEnabled()
                ? "on (ttl=" + humanDuration(cache.getDefaultTtl()) + ")" : "off");
        VeloProperties.JacksonProperties jackson = properties.getJackson();
        line(sb, "jackson", jackson == null ? MISSING_CONFIGURATION : onOff(jackson.isEnabled()));
        VeloProperties.RedisProperties redis = properties.getRedis();
        line(sb, "redis", redis == null ? MISSING_CONFIGURATION : onOff(redis.isEnabled()));
        VeloProperties.MybatisPlusProperties mybatisPlus = properties.getMybatisPlus();
        line(sb, "mybatis-plus", mybatisPlus == null ? MISSING_CONFIGURATION : onOff(mybatisPlus.isEnabled()));
        VeloProperties.ExcelProperties excel = properties.getExcel();
        line(sb, "excel", excel == null || excel.getConverters() == null ? MISSING_CONFIGURATION
                : onOff(excel.getConverters().isEnabled()));
        VeloProperties.LogProperties logProperties = properties.getLog();
        line(sb, "log", logProperties == null ? MISSING_CONFIGURATION : logProperties.isEnabled()
                ? "on (trace=" + traceStatus(logProperties.getTrace())
                        + ", controller=" + sourceStatus(logProperties.getController())
                        + ", feign=" + sourceStatus(logProperties.getFeign()) + ")" : "off");
        VeloProperties.WebProperties web = properties.getWeb();
        line(sb, "web", web == null ? MISSING_CONFIGURATION : onOff(web.isEnabled()));
        line(sb, "xss", xssStatus(properties.getXss()));
        VeloProperties.FeignProperties feign = properties.getFeign();
        line(sb, "feign", feign == null ? MISSING_CONFIGURATION : onOff(feign.isEnabled()));
        sb.append('\n');
        return sb.toString();
    }

    /**
     * 并发能力（幂等/限流/锁）的状态描述：
     * <ul>
     *   <li>关闭：{@code off}</li>
     *   <li>启用但无可用后端（依赖缺失）：{@code on (no backend available)}</li>
     *   <li>用户自定义 handler：{@code custom (类名)}</li>
     *   <li>内置后端：{@code 配置值 -> 实际后端}，例如 {@code AUTO -> Redis}</li>
     * </ul>
     */
    private String concurrency(boolean enabled, ConcurrencyBackend backend, ObjectProvider<?> provider) {
        if (!enabled) {
            return "off";
        }
        Object handler = provider.getIfAvailable();
        if (handler == null) {
            return "on (no backend available)";
        }
        String actual = builtinBackend(handler);
        if (actual == null) {
            return "custom (" + handler.getClass().getSimpleName() + ")";
        }
        return backend + " -> " + actual;
    }

    /**
     * 从内置 handler 类名提取后端简称；非内置实现返回 {@code null}（视为用户自定义）。
     */
    private String builtinBackend(Object handler) {
        String name = handler.getClass().getSimpleName();
        if (name.startsWith("Redisson")) {
            return "Redisson";
        }
        if (name.startsWith("Redis")) {
            return "Redis";
        }
        if (name.startsWith("Caffeine")) {
            return "Caffeine";
        }
        if (name.startsWith("Jdk")) {
            return "Jdk";
        }
        return null;
    }

    private String humanDuration(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return "none";
        }
        long seconds = duration.getSeconds();
        int nanos = duration.getNano();
        if (nanos != 0) {
            if (seconds == 0 && nanos % 1_000_000 == 0) {
                return (nanos / 1_000_000) + "ms";
            }
            return BigDecimal.valueOf(seconds)
                    .add(BigDecimal.valueOf(nanos, 9))
                    .stripTrailingZeros()
                    .toPlainString() + "s";
        }
        if (seconds % 3600 == 0) {
            return (seconds / 3600) + "h";
        }
        if (seconds % 60 == 0) {
            return (seconds / 60) + "m";
        }
        return seconds + "s";
    }

    private String onOff(boolean enabled) {
        return enabled ? "on" : "off";
    }

    private String traceStatus(VeloProperties.TraceProperties trace) {
        return trace == null ? MISSING_CONFIGURATION : onOff(trace.isEnabled());
    }

    private String xssStatus(VeloProperties.XssProperties xss) {
        return xss == null || xss.getStrategy() == null ? MISSING_CONFIGURATION
                : xss.getStrategy() + " (web=" + onOff(xss.isWebEnabled())
                        + ", jackson=" + onOff(xss.isJacksonEnabled()) + ")";
    }

    private String sourceStatus(VeloProperties.InvocationSourceProperties source) {
        return source == null ? MISSING_CONFIGURATION : onOff(source.isEnabled());
    }

    private void line(StringBuilder sb, String name, String value) {
        sb.append("   ").append(String.format("%-13s", name)).append(value).append('\n');
    }
}
