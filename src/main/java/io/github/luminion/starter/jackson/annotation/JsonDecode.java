package io.github.luminion.starter.jackson.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.luminion.starter.jackson.deserializer.JacksonDecodeDeserializer;

import java.lang.annotation.*;
import java.util.function.Function;

/**
 * JSON解码注解，用于自定义字段反序列化时的转换逻辑
 *
 * @author luminion
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonDeserialize(using = JacksonDecodeDeserializer.class)
public @interface JsonDecode {

    /**
     * 指定转换函数类，该类必须实现Function接口
     * <p>
     * 函数签名：Function&lt;String, 字段类型&gt;
     *
     * @return 转换函数类
     */
    Class<? extends Function<String, ?>> value();

}