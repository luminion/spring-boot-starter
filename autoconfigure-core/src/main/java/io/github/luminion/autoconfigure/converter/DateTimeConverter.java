package io.github.luminion.autoconfigure.converter;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * DateTimeFormat注解的转换器
 * 用于处理带有@DateTimeFormat注解的日期时间转换
 *
 * @author luminion
 */
public interface DateTimeConverter<S, T> extends ConditionalConverter, Converter<S, T> {

    @Override
    default boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return !targetType.hasAnnotation(DateTimeFormat.class);
    }

}