package io.github.luminion.velo.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.logging.LogLevel;

/**
 * 异常完成时的类型和提示信息日志；不输出堆栈，不改变异常传播。
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ErrorLog {
    /** 是否记录异常类型和提示信息。 */
    boolean enabled() default true;

    /** 日志级别。 */
    LogLevel level() default LogLevel.WARN;
}
