package io.github.luminion.velo.condition;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 按类型选择可用于 Velo Redis 功能的模板 Bean。
 *
 * <p>候选选择顺序由对应条件实现统一处理：唯一候选、唯一 {@code @Primary} 候选，
 * 最后兼容约定的默认 Bean 名称。多个候选无法确定时不随机选择。
 *
 * @author luminion
 * @since 1.3.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnVeloRedisTemplateCondition.class)
public @interface ConditionalOnVeloRedisTemplate {

    /**
     * 候选 Bean 的类型名称。
     */
    String type();

    /**
     * 多候选且没有 {@code @Primary} 时，用于兼容旧版本的默认 Bean 名称。
     */
    String fallbackBeanName();
}
