package io.github.luminion.velo.core;

import io.github.luminion.velo.ConcurrencyBackend;
import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.idempotent.IdempotentHandler;
import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.ratelimit.RateLimitHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;

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
        if (!properties.getBanner().isEnabled()) {
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
        sb.append(" \\ \\ / / _ \\ |/ _ \\   mode=").append(properties.getMode()).append('\n');
        sb.append("  \\ V /  __/ | (_) |  \n");
        sb.append("   \\_/ \\___|_|\\___/   \n");
        line(sb, "idempotent", concurrency(properties.getIdempotent().isEnabled(),
                properties.getIdempotent().getBackend(), idempotentHandler));
        line(sb, "rate-limit", concurrency(properties.getRateLimit().isEnabled(),
                properties.getRateLimit().getBackend(), rateLimitHandler));
        line(sb, "lock", concurrency(properties.getLock().isEnabled(),
                properties.getLock().getBackend(), lockHandler));
        line(sb, "cache", properties.getCache().isEnabled()
                ? "on (ttl=" + humanDuration(properties.getCache().getDefaultTtl()) + ")" : "off");
        line(sb, "jackson", onOff(properties.getJackson().isEnabled()));
        line(sb, "redis", onOff(properties.getRedis().isEnabled()));
        line(sb, "mybatis-plus", onOff(properties.getMybatisPlus().isEnabled()));
        line(sb, "excel", onOff(properties.getExcel().isEnabled()));
        line(sb, "log", properties.getLog().isEnabled()
                ? "on (trace=" + onOff(properties.getLog().getTrace().isEnabled()) + ")" : "off");
        line(sb, "web", properties.getWeb().isEnabled()
                ? "on (xss=" + onOff(properties.getWeb().getXss().isEnabled()) + ")" : "off");
        line(sb, "feign", onOff(properties.getFeign().isEnabled()));
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

    private void line(StringBuilder sb, String name, String value) {
        sb.append("   ").append(String.format("%-13s", name)).append(value).append('\n');
    }
}
