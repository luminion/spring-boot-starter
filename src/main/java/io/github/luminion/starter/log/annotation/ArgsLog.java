package io.github.luminion.starter.log.annotation;

import java.lang.annotation.*;

/**
 * 入参日志注解
 *
 * @author luminion
 * @since 1.0.0
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ArgsLog {
}
