package io.github.luminion.starter.web.formatter;

import org.springframework.format.Formatter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * LocalDate 格式化器
 *
 * @author luminion
 */
public class LocalDateFormatter implements Formatter<LocalDate> {

    private final DateTimeFormatter formatter;

    public LocalDateFormatter(String pattern) {
        this.formatter = DateTimeFormatter.ofPattern(pattern);
    }

    @Override
    public LocalDate parse(String text, Locale locale) {
        return (text == null || text.isEmpty()) ? null : LocalDate.parse(text, formatter);
    }

    @Override
    public String print(LocalDate object, Locale locale) {
        return (object == null) ? "" : formatter.format(object);
    }
}
