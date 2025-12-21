package io.github.luminion.autoconfigure.web.converter.support;

import io.github.luminion.autoconfigure.web.converter.DateTimeConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @author luminion
 */
public class StringToLocalDateConverter implements DateTimeConverter<String, LocalDate> {
    private final DateTimeFormatter formatter;

    public StringToLocalDateConverter(String pattern) {
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
