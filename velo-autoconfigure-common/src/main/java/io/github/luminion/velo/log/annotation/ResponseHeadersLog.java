package io.github.luminion.velo.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.logging.LogLevel;

/**
 * HTTP 响应头日志。
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseHeadersLog {
    /** 是否记录 HTTP 响应头。 */
    boolean enabled() default true;

    /** 日志级别。 */
    LogLevel level() default LogLevel.INFO;

    /** 允许记录的头名称，忽略大小写匹配；空数组表示全部头。 */
    String[] allowlist() default {};
}
