package io.github.luminion.starter.web.formatter;

import io.github.luminion.starter.xss.XssCleaner;
import lombok.RequiredArgsConstructor;
import org.springframework.format.Formatter;

import java.util.Locale;

/**
 * XSS 清理格式化器
 *
 * @author luminion
 */
@RequiredArgsConstructor
public class XssFormatter implements Formatter<String> {

    private final XssCleaner xssCleaner;

    @Override
    public String parse(String text, Locale locale) {
        return (text != null) ? xssCleaner.clean(text) : null;
    }

    @Override
    public String print(String object, Locale locale) {
        return object;
    }
}
