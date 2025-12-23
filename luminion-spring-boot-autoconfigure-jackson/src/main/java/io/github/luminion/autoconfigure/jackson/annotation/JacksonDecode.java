package io.github.luminion.autoconfigure.jackson.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.luminion.autoconfigure.jackson.deserializer.JacksonDecodeDeserializer;

import java.lang.annotation.*;
import java.util.function.Function;

/**
 * @author luminion
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonDeserialize(using = JacksonDecodeDeserializer.class)
public @interface JacksonDecode {

    Class<? extends Function<String, ?>> value() default Fallback.class;

    class Fallback implements Function<String, Object> {
        @Override
        public Object apply(String s) {
            return s;
        }
    }

}