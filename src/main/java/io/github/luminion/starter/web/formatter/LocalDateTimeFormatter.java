package io.github.luminion.starter.web.formatter;

import org.springframework.format.Formatter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * LocalDateTime 格式化器
 *
 * @author luminion
 */
public class LocalDateTimeFormatter implements Formatter<LocalDateTime> {

    private final DateTimeFormatter formatter;

    public LocalDateTimeFormatter(String pattern) {
        this.formatter = DateTimeFormatter.ofPattern(pattern);
    }

    @Override
    public LocalDateTime parse(String text, Locale locale) {
        return (text == null || text.isEmpty()) ? null : LocalDateTime.parse(text, formatter);
    }

    @Override
    public String print(LocalDateTime object, Locale locale) {
        return (object == null) ? "" : formatter.format(object);
    }
}
