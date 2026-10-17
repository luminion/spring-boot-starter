package io.github.luminion.velo.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 达到阈值时的慢调用日志；独立于 {@link CostLog}，不联动采集参数或结果。 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SlowLog {
    /** 是否记录达到阈值的调用耗时。 */
    boolean enabled() default true;

    /** 日志级别；未指定时逐级继承。 */
    LogPolicyLevel level() default LogPolicyLevel.INHERIT;

    /** 耗时阈值，单位为毫秒；-1 表示继承，0 表示记录全部耗时，其他值必须大于 0。 */
    long threshold() default -1L;
}
