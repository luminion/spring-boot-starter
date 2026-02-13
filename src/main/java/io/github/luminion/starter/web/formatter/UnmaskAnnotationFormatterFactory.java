package io.github.luminion.starter.web.formatter;

import io.github.luminion.starter.converter.XssCleanerConverter;
import io.github.luminion.starter.mask.annotation.Unmask;
import io.github.luminion.starter.xss.XssCleaner;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
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
        // 1. 从容器中获取 @Unmask 指定的解密/还原策略 Bean
        Class<? extends Function<String, String>> unmaskerClass = annotation.value();
        Function<String, String> unmasker = applicationContext.getBean(unmaskerClass);

        // 2. 【核心改造】构建逻辑链：先解密，后清洗
        // 如果注入了 xssCleaner，则将两个步骤串联起来
        Function<String, String> compositeLogic = unmasker;

        ObjectProvider<XssCleaner> xssCleanerObjectProvider = applicationContext.getBeanProvider(XssCleaner.class);
        XssCleaner xssCleaner = xssCleanerObjectProvider.getIfAvailable();
        if (xssCleaner != null) {
            // .andThen() 意味着：先执行 unmasker.apply(input)，
            // 然后把结果传给 xssCleaner.clean(result)
            compositeLogic = unmasker.andThen(xssCleaner::clean);
        }

        // 3. 将组合后的“复合函数”传给你的 Formatter
        return new StringFunctionFormatter(compositeLogic);
    }
}
