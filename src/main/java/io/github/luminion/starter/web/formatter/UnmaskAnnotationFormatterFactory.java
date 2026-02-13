package io.github.luminion.starter.web.formatter;

import io.github.luminion.starter.core.annotation.Unmask;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Parser;
import org.springframework.format.Printer;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

/**
 * {@link Unmask} 注解格式化工厂
 *
 * @author luminion
 */
@RequiredArgsConstructor
public class UnmaskAnnotationFormatterFactory implements AnnotationFormatterFactory<Unmask> {

    private final ApplicationContext applicationContext;

    @Override
    public Set<Class<?>> getFieldTypes() {
        return Collections.singleton(String.class);
    }

    @Override
    public Printer<?> getPrinter(Unmask annotation, Class<?> fieldType) {
        return (obj, locale) -> (String) obj;
    }

    @Override
    public Parser<?> getParser(Unmask annotation, Class<?> fieldType) {
        Class<? extends Function<String, String>> unmaskerClass = annotation.value();
        Function<String, String> unmasker = applicationContext.getBean(unmaskerClass);
        return new StringFunctionFormatter(unmasker);
    }
}
