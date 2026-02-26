package io.github.luminion.starter.jackson.annotation;

import java.lang.annotation.*;

/**
 * JSON枚举翻译注解
 * <p>
 * 用于字段（成员变量），在 Jackson 序列化时自动在 JSON 中增加一个描述字段。
 *
 * @author luminion
 * @since 1.0.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonEnum {

    /**
     * 绑定的枚举类
     */
    Class<? extends Enum<?>> value();

    /**
     * 枚举中作为 Key 的属性名
     */
    String keyField() default "";

    /**
     * 枚举中作为 Label (翻译值) 的属性名
     */
    String labelField() default "";
    
}
