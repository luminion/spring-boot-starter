package io.github.luminion.starter.core.annotation;

import java.lang.annotation.*;
import java.util.function.Function;

/**
 * JSON编码注解，用于自定义字段序列化时的转换逻辑
 * <p>
 * 仅支持从 String 到 String 的转换（如脱敏）
 *
 * @author luminion
 * @since 1.0.0
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface StringEncode {

    /**
     * 指定转换函数类，该类必须注册为 Spring Bean
     * <p>
     * 函数签名：Function&lt;String, String&gt;
     *
     * @return 转换函数类
     */
    Class<? extends Function<String, String>> value();

}