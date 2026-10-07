package io.github.luminion.velo.condition;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 根据列表配置属性是否存在及是否为空决定是否匹配。
 *
 * @author luminion
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnListPropertyCondition.class)
public @interface ConditionalOnListProperty {
    
    /** 列表配置的完整属性路径，必须显式指定，不解析 SpEL。 */
    String value();

    /**
     * 列表为空时是否匹配
     *
     * @return boolean
     */
    boolean matchIfEmpty() default false;

    /**
     * 属性缺失时是否匹配（建议与 matchIfEmpty 区分开）
     * @return  boolean
     */
    boolean matchIfMissing() default false;

    /**
     * 可选：元素类型（默认字符串）
     *
     * @return Class
     */
    Class<?> elementType() default String.class;

    /**
     * 可选：忽略空白元素（例如 "", "  "）
     *
     * @return boolean
     */
    boolean ignoreEmptyElements() default false;
}
