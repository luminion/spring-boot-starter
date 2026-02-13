package io.github.luminion.starter.log.annotation;

import org.slf4j.event.Level;

import java.lang.annotation.*;

/**
 * @author luminion
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TimeLog {

    /**
     * 执行者名称
     * 默认为类名+方法名
     */
    String value() default "";

    /**
     * 阈值(毫秒),超过阈值则记录日志
     */
    int threshold() default 0;

    /**
     * 日志等级
     */
    Level level() default Level.INFO;
}
