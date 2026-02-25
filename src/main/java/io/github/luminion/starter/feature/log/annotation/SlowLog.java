package io.github.luminion.starter.feature.log.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 慢日志记录注解
 * 用于标识需要监控执行时间的业务方法或类
 *
 * @author luminion
 * @since 1.0.0
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SlowLog {

    /**
     * 耗时阈值
     * 超过该值则记录日志
     */
    int value() default 200;

    /**
     * 阈值时间单位
     * 默认毫秒
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
}
