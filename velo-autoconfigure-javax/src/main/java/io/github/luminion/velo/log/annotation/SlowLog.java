package io.github.luminion.velo.log.annotation;

import java.lang.annotation.*;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SlowLog {

    /**
     * 慢调用阈值，单位为毫秒。
     */
    long value() default 200;
}
