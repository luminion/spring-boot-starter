package io.github.luminion.velo.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.logging.LogLevel;

/** 达到阈值时的耗时日志；不联动采集参数或结果。 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SlowLog {
    /** 是否记录达到阈值的调用耗时。 */
    boolean enabled() default true;

    /** 日志级别。 */
    LogLevel level() default LogLevel.WARN;

    /** 耗时阈值，单位为毫秒；0 表示记录全部耗时，必须大于或等于 0。 */
    long threshold() default 1000L;
}
