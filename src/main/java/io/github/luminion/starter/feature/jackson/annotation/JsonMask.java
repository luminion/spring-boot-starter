package io.github.luminion.starter.feature.jackson.annotation;

import io.github.luminion.starter.core.spi.StringMasker;
import io.github.luminion.starter.core.spi.masker.SimpleMasker;

import java.lang.annotation.*;

/**
 * json字段脱敏，用于自定义字段序列化时的转换逻辑
 * <p>
 *
 * @author luminion
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonMask {

    Class<? extends StringMasker> value() default SimpleMasker.class;

}