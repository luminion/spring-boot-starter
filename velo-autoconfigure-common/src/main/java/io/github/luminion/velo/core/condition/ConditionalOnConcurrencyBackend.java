package io.github.luminion.velo.core.condition;

import io.github.luminion.velo.core.ConcurrencyBackend;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Matches a concurrency backend in either explicit or auto-selection mode.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnConcurrencyBackendCondition.class)
public @interface ConditionalOnConcurrencyBackend {

    /**
     * Feature property prefix, for example {@code velo.lock}.
     */
    String prefix();

    /**
     * Backend represented by the annotated configuration or bean.
     */
    ConcurrencyBackend value();

    /**
     * Whether AUTO backend selection can match this condition.
     */
    boolean matchAuto() default true;

    /**
     * Classes that must be present only when backend selection is AUTO.
     */
    String[] autoClassNames() default {};

    /**
     * Bean names that must be present only when backend selection is AUTO.
     */
    String[] autoBeanNames() default {};

    /**
     * Bean types that must be present only when backend selection is AUTO.
     */
    String[] autoBeanTypeNames() default {};
}
