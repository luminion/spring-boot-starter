package io.github.luminion.velo.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.logging.LogLevel;

/**
 * 调用完成时的参数状态日志。
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExitArgs {
    /** 是否记录调用结束时的参数状态。 */
    boolean enabled() default true;

    /** 日志级别。 */
    LogLevel level() default LogLevel.INFO;
}
