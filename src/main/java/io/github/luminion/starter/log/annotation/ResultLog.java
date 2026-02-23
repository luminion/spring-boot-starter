package io.github.luminion.starter.log.annotation;

import java.lang.annotation.*;

/**
 * 返回值日志注解
 *
 * @author luminion
 * @since 1.0.0
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResultLog {
}
