package io.github.luminion.starter.jackson.annotation;

import java.lang.annotation.*;
import java.util.function.Function;

/**
 * JSON字段加密，用于自定义字段序列化时的转换逻辑
 *
 * @author luminion
 * @since 1.0.0
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonEncode {

    /**
     * 指定转换函数类
     *
     */
    Class<? extends Function<String,String>> value();

}