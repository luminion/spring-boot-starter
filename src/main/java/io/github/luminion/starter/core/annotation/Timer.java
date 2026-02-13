package io.github.luminion.starter.core.annotation;

import java.lang.annotation.*;

/**
 * @author luminion
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Timer {

    /**
     * 计时名称
     * 默认为类名.方法名
     *
     * @return 字符串
     */
    String value() default "";
}
