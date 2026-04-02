package io.github.luminion.velo.log.annotation;

import java.lang.annotation.*;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgsLog
@ResultLog
@ErrorLog
public @interface InvokeLog {
}
