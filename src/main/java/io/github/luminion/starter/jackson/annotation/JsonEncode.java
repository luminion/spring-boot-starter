package io.github.luminion.starter.jackson.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.luminion.starter.jackson.serializer.JacksonEncodeSerializer;

import java.lang.annotation.*;
import java.util.function.Function;

/**
 * JSON编码注解，用于自定义字段序列化时的转换逻辑
 *
 * @author luminion
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonSerialize(using = JacksonEncodeSerializer.class)
public @interface JsonEncode {

    /**
     * 指定转换函数类，该类必须实现Function接口
     * <p>
     * 函数签名：Function&lt;字段类型, String&gt;
     *
     * @return 转换函数类
     */
    Class<? extends Function<?, String>> value();

}