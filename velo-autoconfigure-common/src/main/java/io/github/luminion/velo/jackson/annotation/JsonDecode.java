package io.github.luminion.velo.jackson.annotation;

import java.lang.annotation.*;
import java.util.function.Function;

@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonDecode {

    Class<? extends Function<String, String>> value();

}
