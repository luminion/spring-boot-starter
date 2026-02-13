package io.github.luminion.starter.web.formatter;

import org.springframework.format.Formatter;

import java.util.Locale;
import java.util.function.Function;

/**
 * 通用字符串转换格式化器
 *
 * @author luminion
 */
public class StringFunctionFormatter implements Formatter<String> {

    private final Function<String, String> function;

    public StringFunctionFormatter(Function<String, String> function) {
        this.function = function;
    }

    @Override
    public String parse(String text, Locale locale) {
        return (text != null && function != null) ? function.apply(text) : text;
    }

    @Override
    public String print(String object, Locale locale) {
        return object;
    }
}
