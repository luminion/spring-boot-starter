package io.github.luminion.starter.converter;

import io.github.luminion.starter.xss.XssCleaner;
import io.github.luminion.starter.xss.XssIgnore;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.Converter;

/**
 * @author luminion
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class XssCleanerConverter implements Converter<String, String>, ConditionalConverter {
    private final XssCleaner xssCleaner;
    
    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return  !targetType.hasAnnotation(XssIgnore.class);
    }

    @Override
    public String convert(String source) {
        if (source.isEmpty()) {
            return source;
        }
        return xssCleaner.clean(source);
    }
}
