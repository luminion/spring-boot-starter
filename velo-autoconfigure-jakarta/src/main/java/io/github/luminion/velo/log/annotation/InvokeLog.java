package io.github.luminion.velo.log.annotation;

import java.lang.annotation.*;

/**
 * Enables unified invocation logging for a method or type.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InvokeLog {

    /**
     * Whether to include the arguments' current state when the invocation finishes,
     * including both normal return and exceptional completion.
     *
     * @return {@code true} to include arguments in the finish record
     */
    boolean argsOnFinish() default false;
}
