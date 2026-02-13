package io.github.luminion.starter.jackson.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.luminion.starter.jackson.deserializer.JacksonXssDeserializer;

import java.lang.annotation.*;

/**
 * 允许跨域内容
 *
 * @author luminion
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonDeserialize(using = JacksonXssDeserializer.class)
public @interface JsonXss {

}