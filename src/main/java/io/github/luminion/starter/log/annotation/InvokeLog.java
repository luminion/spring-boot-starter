package io.github.luminion.starter.log.annotation;

import java.lang.annotation.*;

/**
 * 日志记录
 * 
 * @author luminion
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InvokeLog {
    /**
     * 是否记录入参
     */
    boolean logArgs() default true;

    /**
     * 是否记录返回值
     */
    boolean logResult() default true;
}
