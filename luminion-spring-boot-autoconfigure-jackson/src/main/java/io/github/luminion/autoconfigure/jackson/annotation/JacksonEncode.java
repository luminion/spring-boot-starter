package io.github.luminion.autoconfigure.jackson.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.luminion.autoconfigure.jackson.serializer.JacksonEncodeSerializer;

import java.lang.annotation.*;
import java.util.function.Function;

/**
 * @author luminion
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonSerialize(using = JacksonEncodeSerializer.class)
public @interface JacksonEncode {

    Class<? extends Function<?, String>> value() default Fallback.class;

    class Fallback implements Function<Object, String> {
        @Override
        public String apply(Object s) {
            return s.toString();
        }
    }

}