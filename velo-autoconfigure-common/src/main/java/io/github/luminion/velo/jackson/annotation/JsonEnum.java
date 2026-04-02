package io.github.luminion.velo.jackson.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonEnum {

    Class<? extends Enum<?>> value();

    String keyField() default "";

    String labelField() default "";

}
