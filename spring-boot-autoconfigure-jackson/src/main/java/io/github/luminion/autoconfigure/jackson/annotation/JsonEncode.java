package io.github.luminion.autoconfigure.jackson.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.luminion.autoconfigure.jackson.serializer.JacksonEncodeSerializer;

import java.lang.annotation.*;
import java.util.function.Function;

/**
 * JSON编码注解，用于自定义字段序列化时的转换逻辑
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 使用自定义转换器类
 * @JsonEncode(PhoneEncoder.class)
 * private String phone;
 *
 * // 或者使用lambda表达式的简化类
 * public class SimpleEncoder implements Function<String, String> {
 *     @Override
 *     public String apply(String s) {
 *         return s.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
 *     }
 * }
 * }</pre>
 * <p>
 * <b>设计说明：</b>
 * <ul>
 *     <li>当前实现使用Function类的方式，提供了最大的灵活性和扩展性，可以支持任何自定义转换逻辑</li>
 *     <li>这种方式符合函数式编程思想，便于复用和组合</li>
 *     <li>如果觉得这种方式过于复杂，可以考虑提供基于枚举的简化版本（如RuoYi的@Sensitive注解方式）</li>
 *     <li>但基于枚举的方式灵活性较差，难以处理复杂的转换场景</li>
 * </ul>
 * <p>
 * <b>与RuoYi框架的@Sensitive注解对比：</b>
 * <ul>
 *     <li>优点：灵活性高，支持任意自定义转换逻辑，易于扩展</li>
 *     <li>缺点：需要为每个转换创建单独的类，使用稍显繁琐</li>
 *     <li>适用场景：需要复杂转换逻辑或自定义转换的场景</li>
 * </ul>
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