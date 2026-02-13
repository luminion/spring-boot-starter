package io.github.luminion.starter.web.formatter;

import org.springframework.format.Formatter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * LocalTime 格式化器
 *
 * @author luminion
 */
public class LocalTimeFormatter implements Formatter<LocalTime> {

    private final DateTimeFormatter formatter;

    public LocalTimeFormatter(String pattern) {
        this.formatter = DateTimeFormatter.ofPattern(pattern);
    }

    @Override
    public LocalTime parse(String text, Locale locale) {
        return (text == null || text.isEmpty()) ? null : LocalTime.parse(text, formatter);
    }

    @Override
    public String print(LocalTime object, Locale locale) {
        return (object == null) ? "" : formatter.format(object);
    }
}
