package io.github.luminion.velo.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 当前调用正常返回或抛出异常时的耗时日志，不受慢调用阈值影响。
 * 只记录当前同步调用点，不等待返回对象或方法内部异步任务完成，不联动采集参数或结果。
 * 可独立接入普通方法日志，不联动开启其他功能。
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CostLog {
    /** 是否记录当前调用的耗时。 */
    boolean enabled() default true;

    /** 日志级别；未指定时逐级继承，独立于慢调用及异常日志。 */
    LogPolicyLevel level() default LogPolicyLevel.INHERIT;
}
