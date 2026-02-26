package io.github.luminion.starter.jackson.annotation;

import io.github.luminion.starter.core.spi.StringEncryptor;

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
public @interface JsonEncrypt {

    /**
     * 指定转换函数类，该类必须注册为 Spring Bean
     *
     */
    Class<? extends StringEncryptor> value();

}