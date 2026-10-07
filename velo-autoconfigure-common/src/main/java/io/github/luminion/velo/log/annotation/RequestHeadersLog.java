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
 * HTTP 请求头日志。
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestHeadersLog {
    /** 常用属性 allowlist 的简写。 */
    @AliasFor("allowlist")
    String[] value() default {};

    boolean enabled() default true;

    LogLevel level() default LogLevel.INFO;

    @AliasFor("value")
    String[] allowlist() default {};
}
