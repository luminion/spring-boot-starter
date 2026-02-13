package io.github.luminion.starter.web.formatter;

import io.github.luminion.starter.mask.annotation.Mask;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Parser;
import org.springframework.format.Printer;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

/**
 * {@link Mask} 注解格式化工厂
 *
 * @author luminion
 */
@RequiredArgsConstructor
public class MaskAnnotationFormatterFactory implements AnnotationFormatterFactory<Mask> {

    private final ApplicationContext applicationContext;

    @Override
    public Set<Class<?>> getFieldTypes() {
        return Collections.singleton(String.class);
    }

    @Override
    public Printer<?> getPrinter(Mask annotation, Class<?> fieldType) {
        return (obj, locale) -> (String) obj;
    }

    @Override
    public Parser<?> getParser(Mask annotation, Class<?> fieldType) {
        Class<? extends Function<String, String>> maskerClass = annotation.value();
        Function<String, String> masker = applicationContext.getBean(maskerClass);
        return new StringFunctionFormatter(masker);
    }
}
