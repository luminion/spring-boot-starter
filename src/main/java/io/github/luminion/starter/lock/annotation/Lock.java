package io.github.luminion.starter.lock.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 *
 * @author luminion
 * @since 1.0.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lock {

    /**
     * 锁的 Key (支持 SpEL 表达式)
     */
    String value() default "";

    /**
     * 等待获取锁的时间
     * 默认 0 (不等待，获取不到立即失败)
     */
    long waitTime() default 0;

    /**
     * 锁的持有时间 (自动释放时间)
     * 默认 30 秒，防止死锁
     */
    long leaseTime() default 30;

    /**
     * 时间单位
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * 失败提示信息
     */
    String message() default "系统繁忙，请稍后再试";

}
