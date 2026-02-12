package io.github.luminion.starter.support.log.annotation;

import java.lang.annotation.*;

/**
 * 日志记录
 * @author luminion
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    
}
