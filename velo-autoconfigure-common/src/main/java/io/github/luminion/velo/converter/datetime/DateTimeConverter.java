package io.github.luminion.velo.converter.datetime;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 解析 {@code DateTimeFormat} 注解并执行日期时间转换。
 * 用于兼容@DateTimeFormat注解的日期时间转换
 *
 * @author luminion
 */
public interface DateTimeConverter<S, T> extends ConditionalConverter, Converter<S, T> {

    @Override
    default boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return !targetType.hasAnnotation(DateTimeFormat.class);
    }

}
