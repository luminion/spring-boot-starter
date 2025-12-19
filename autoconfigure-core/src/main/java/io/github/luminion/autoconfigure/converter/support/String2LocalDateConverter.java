package io.github.luminion.autoconfigure.converter.support;

import io.github.luminion.autoconfigure.converter.DateTimeConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @author luminion
 */
public class String2LocalDateConverter implements DateTimeConverter<String, LocalDate> {
    private final DateTimeFormatter formatter;

    public String2LocalDateConverter(String pattern) {
        this.formatter = DateTimeFormatter.ofPattern(pattern);
    }

    @Override
    public LocalDate convert(String source) {
        if (source.isEmpty()) {
            return null;
        }
        return LocalDate.parse(source, formatter);
    }
}
