package io.github.luminion.velo.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import java.util.function.Supplier;

/**
 * {@link ObjectProvider} 可选依赖解析工具。
 *
 * <p>{@link ObjectProvider#getIfAvailable} 在存在多个候选且无 {@code @Primary} 时会抛出
 * {@code NoUniqueBeanDefinitionException}，导致应用启动失败；本工具统一为"单候选/主候选直接使用、
 * 多候选互斥时回退默认并告警"的语义，符合可选依赖"有则用、无则兜底"的预期。</p>
 *
 * @author luminion
 * @since 1.3.2
 */
public final class ObjectProviderSupport {

    private static final Logger log = LoggerFactory.getLogger(ObjectProviderSupport.class);

    private ObjectProviderSupport() {
    }

    /**
     * 解析唯一的可选依赖。
     *
     * <p>存在单个候选或标记 {@code @Primary}/{@code @Priority} 的候选时返回该候选；
     * 无候选时回退 {@code fallback}（debug 日志）；多候选且无法决出主候选时同样回退 {@code fallback}，
     * 并打 WARN 提示用户消除歧义。</p>
     *
     * @param provider bean 提供者
     * @param usage    用途描述，用于日志定位
     * @param fallback 无候选或候选歧义时的默认值提供者
     * @param <T>      bean 类型
     * @return 解析结果，永不为 null（除非 fallback 返回 null）
     */
    public static <T> T resolveUnique(ObjectProvider<T> provider, String usage, Supplier<T> fallback) {
        T resolved = provider.getIfUnique();
        if (resolved != null) {
            return resolved;
        }
        long candidateCount = provider.stream().count();
        if (candidateCount > 1) {
            log.warn("[Velo Starter] Found {} candidate beans with no @Primary for {}; falling back to the default. "
                    + "Mark one with @Primary or remove the redundant beans to control the choice.", candidateCount,
                    usage);
        } else {
            log.debug("No {} bean found, falling back to the default", usage);
        }
        return fallback.get();
    }
}
