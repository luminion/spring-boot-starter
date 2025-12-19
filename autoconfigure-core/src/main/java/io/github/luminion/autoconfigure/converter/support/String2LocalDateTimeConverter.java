package io.github.luminion.autoconfigure.converter.support;

import io.github.luminion.autoconfigure.converter.DateTimeConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author luminion
 */
public class String2LocalDateTimeConverter implements DateTimeConverter<String, LocalDateTime> {
    private final DateTimeFormatter formatter;

    public String2LocalDateTimeConverter(String pattern) {
        this.formatter = DateTimeFormatter.ofPattern(pattern);
    }

    @Override
    public LocalDateTime convert(String source) {
        if (source.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(source, formatter);
    }

}
