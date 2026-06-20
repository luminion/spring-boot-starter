package io.github.luminion.velo.log.annotation;

import java.lang.annotation.*;

/**
 * Controls payload visibility in invocation logs.
 * <p>
 * When present on a method or class, the annotated method's arguments and/or
 * return value are replaced with {@code "-"} in log output. The invocation
 * itself is still logged (method name, cost, success/error status).
 * <p>
 * Works across all Velo log aspects: {@code ControllerLogAspect},
 * {@code FeignLogAspect}, {@code InvokeLogAspect} and {@code SlowLogAspect}.
 * <p>
 * There is no conflict with {@code @SlowLog}: when a slow invocation is
 * detected, the log record is still emitted, but args/result show as
 * {@code "-"} if {@code @LogPayloadIgnore} is present.
 *
 * <pre>{@code
 * // Skip both args and result
 * @LogPayloadIgnore
 * public void deleteUser(Long userId) { ... }
 *
 * // Skip only result
 * @LogPayloadIgnore(args = false)
 * public UserDTO login(LoginRequest req) { ... }
 * }</pre>
 *
 * @author luminion
 * @since 1.3.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogPayloadIgnore {

    /**
     * Whether to suppress argument logging for the annotated method.
     * <p>
     * Defaults to {@code true}.
     */
    boolean args() default true;

    /**
     * Whether to suppress return value logging for the annotated method.
     * <p>
     * Defaults to {@code true}.
     */
    boolean result() default true;
}
