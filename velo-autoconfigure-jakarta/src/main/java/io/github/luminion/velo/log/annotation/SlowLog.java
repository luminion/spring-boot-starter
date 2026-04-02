package io.github.luminion.velo.log.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SlowLog {

    int value() default 200;

    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
}
