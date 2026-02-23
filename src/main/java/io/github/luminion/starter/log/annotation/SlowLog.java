package io.github.luminion.starter.log.annotation;

import org.slf4j.event.Level;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @author luminion
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SlowLog {

    /**
     * 执行者名称
     * 
     * @return 名称
     */
    String value() default "";

    /**
     * 执行者名称
     */
    String name() default "";

    /**
     * 耗时阈值，超过阈值则记录日志
     */
    int threshold() default 200;

    /**
     * 阈值时间单位
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    /**
     * 日志等级
     *
     * @return 级别
     */
    Level level() default Level.DEBUG;
}
