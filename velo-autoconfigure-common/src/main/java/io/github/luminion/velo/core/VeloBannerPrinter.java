package io.github.luminion.velo.core;

import io.github.luminion.velo.VeloProperties;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * 在 {@code velo.banner.enabled=true}（默认开启）时打印 Velo 启动横幅与各功能开关概览。
 * <p>
 * 与 Spring Boot 的 banner 一致，直接输出到控制台（{@code System.out}），
 * 不经过日志框架，因此不进入日志文件、也不受日志级别控制。
 *
 * @author luminion
 */
public class VeloBannerPrinter implements SmartInitializingSingleton {

    private final VeloProperties properties;

    public VeloBannerPrinter(VeloProperties properties) {
        this.properties = properties;
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
        sb.append(" \\ \\ / / _ \\ |/ _ \\   mode: ").append(properties.getMode()).append('\n');
        sb.append("  \\ V /  __/ | (_) |  \n");
        sb.append("   \\_/ \\___|_|\\___/   features:\n");
        appendFeature(sb, "idempotent", properties.getIdempotent().isEnabled(),
                "backend=" + properties.getIdempotent().getBackend());
        appendFeature(sb, "rate-limit", properties.getRateLimit().isEnabled(),
                "backend=" + properties.getRateLimit().getBackend());
        appendFeature(sb, "lock", properties.getLock().isEnabled(),
                "backend=" + properties.getLock().getBackend());
        appendFeature(sb, "cache", properties.getCache().isEnabled(),
                "default-ttl=" + properties.getCache().getDefaultTtl());
        appendFeature(sb, "jackson", properties.getJackson().isEnabled(), "");
        appendFeature(sb, "redis", properties.getRedis().isEnabled(), "");
        appendFeature(sb, "mybatis-plus", properties.getMybatisPlus().isEnabled(), "");
        appendFeature(sb, "excel", properties.getExcel().isEnabled(), "");
        appendFeature(sb, "log", properties.getLog().isEnabled(),
                "trace=" + properties.getLog().getTrace().isEnabled());
        appendFeature(sb, "web", properties.getWeb().isEnabled(),
                "xss=" + properties.getWeb().getXss().isEnabled());
        appendFeature(sb, "feign", properties.getFeign().isEnabled(), "");
        sb.append('\n');
        return sb.toString();
    }

    private void appendFeature(StringBuilder sb, String name, boolean enabled, String detail) {
        sb.append("   - ").append(name);
        sb.append(enabled ? " [on]" : " [off]");
        if (detail != null && !detail.isEmpty()) {
            sb.append(' ').append(detail);
        }
        sb.append('\n');
    }
}
