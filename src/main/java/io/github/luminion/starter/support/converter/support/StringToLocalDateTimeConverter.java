package io.github.luminion.starter.support.converter.support;

import io.github.luminion.starter.support.converter.DateTimeConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author luminion
 */
public class StringToLocalDateTimeConverter implements DateTimeConverter<String, LocalDateTime> {
    private final DateTimeFormatter formatter;

    public StringToLocalDateTimeConverter(String pattern) {
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
