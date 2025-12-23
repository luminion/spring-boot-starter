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
public @interface JsonDecode {

    Class<? extends Function<String, ?>> value();

}