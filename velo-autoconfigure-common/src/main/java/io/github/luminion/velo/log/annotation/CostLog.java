package io.github.luminion.velo.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.logging.LogLevel;

/**
 * 当前调用正常返回或抛出异常时的耗时日志，不受慢调用阈值影响。
 * 只记录当前同步调用点，不等待返回对象或方法内部异步任务完成，不联动采集参数或结果。
 * 普通方法仍需使用 {@link InvokeLog} 接入日志切面。
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CostLog {
    /** 是否记录当前调用的耗时。 */
    boolean enabled() default true;

    /** 日志级别，独立于慢调用及异常日志的级别。 */
    LogLevel level() default LogLevel.INFO;
}
