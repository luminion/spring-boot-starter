package io.github.luminion.velo.condition;

import io.github.luminion.velo.ConcurrencyBackend;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 匹配显式选择或自动选择的并发控制后端。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnConcurrencyBackendCondition.class)
public @interface ConditionalOnConcurrencyBackend {

    /**
     * 功能配置属性的前缀，例如 {@code velo.lock}，不是业务资源键前缀。
     */
    String prefix();

    /**
     * 当前配置或 Bean 所代表的后端。
     */
    ConcurrencyBackend backend();

    /**
     * 是否参与 AUTO 模式的后端匹配。
     */
    boolean matchAuto() default true;

    /**
     * AUTO 模式下必须存在的类名称。
     */
    String[] autoClassNames() default {};

    /**
     * AUTO 模式下必须存在的 Bean 名称。
     */
    String[] autoBeanNames() default {};

    /**
     * AUTO 模式下必须存在的 Bean 类型名称。
     */
    String[] autoBeanTypeNames() default {};
}
