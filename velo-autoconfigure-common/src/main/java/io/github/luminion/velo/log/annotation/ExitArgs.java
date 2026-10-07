package io.github.luminion.velo.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;

import org.springframework.boot.logging.LogLevel;

/**
 * 调用完成时的参数状态日志。
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExitArgs {
    /** 常用属性 level 的简写。 */
    @AliasFor("level")
    LogLevel value() default LogLevel.INFO;

    boolean enabled() default true;

    @AliasFor("value")
    LogLevel level() default LogLevel.INFO;
}
