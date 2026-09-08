package io.github.luminion.velo.redis;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.ListableBeanFactory;

/**
 * 统一解析 Velo Redis 功能使用的模板 Bean。
 *
 * <p>优先使用唯一候选或唯一 {@code @Primary} 候选；无法通过类型唯一确定时，
 * 兼容使用约定的默认 Bean 名称。该类不会在多个候选中随机选择。
 *
 * @author luminion
 * @since 1.3.1
 */
public final class VeloRedisTemplateResolver {

    private VeloRedisTemplateResolver() {
    }

    /**
     * 解析 Redis 模板候选 Bean。
     *
     * @param provider           按目标模板类型注入的候选提供器
     * @param beanFactory        Bean 工厂，用于默认名称兜底
     * @param beanTypeName       目标模板类型名称，仅用于异常信息
     * @param fallbackBeanName   兼容旧版本的默认 Bean 名称
     * @param <T>                模板类型
     * @return 选中的模板 Bean
     */
    @SuppressWarnings("unchecked")
    public static <T> T resolve(ObjectProvider<T> provider, ListableBeanFactory beanFactory,
            String beanTypeName, String fallbackBeanName) {
        try {
            T candidate = provider.getIfUnique();
            if (candidate != null) {
                return candidate;
            }
        } catch (NoUniqueBeanDefinitionException ex) {
            throw new IllegalStateException("无法唯一确定 Velo Redis 模板 Bean（类型：" + beanTypeName + "）", ex);
        }

        if (beanFactory.containsBean(fallbackBeanName)) {
            return (T) beanFactory.getBean(fallbackBeanName);
        }

        throw new NoSuchBeanDefinitionException(beanTypeName,
                "未找到唯一的 Velo Redis 模板 Bean，且默认 Bean 名称不存在：" + fallbackBeanName);
    }
}
