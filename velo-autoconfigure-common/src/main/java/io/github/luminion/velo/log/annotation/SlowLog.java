package io.github.luminion.velo.log.annotation;

import io.github.luminion.velo.log.model.LogPolicyLevel;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 达到阈值时记录慢调用摘要，与常规耗时日志独立。
 * 每条声明包含完整默认值，不从其他声明逐项继承属性。
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SlowLog {
    /**
     * 本功能使用的日志级别，OFF 关闭该功能。
     */
    LogPolicyLevel level() default LogPolicyLevel.WARN;

    /**
     * 慢调用阈值，单位毫秒；0 表示记录全部调用。
     */
    long threshold() default 1000L;
}
