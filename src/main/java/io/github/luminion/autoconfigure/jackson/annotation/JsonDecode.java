package io.github.luminion.autoconfigure.jackson.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.luminion.autoconfigure.jackson.deserializer.JacksonDecodeDeserializer;

import java.lang.annotation.*;
import java.util.function.Function;

/**
 * JSON解码注解，用于自定义字段反序列化时的转换逻辑
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 使用自定义转换器类
 * @JsonDecode(StringToLongDecoder.class)
 * private Long id;
 *
 * // 转换器类示例
 * public class StringToLongDecoder implements Function<String, Long> {
 *     @Override
 *     public Long apply(String s) {
 *         return Long.parseLong(s);
 *     }
 * }
 * }</pre>
 * <p>
 * <b>设计说明：</b>
 * <ul>
 *     <li>当前实现使用Function类的方式，提供了最大的灵活性和扩展性</li>
 *     <li>可以支持从String到任意类型的转换</li>
 *     <li>与@JsonEncode配合使用，可以实现双向转换</li>
 * </ul>
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