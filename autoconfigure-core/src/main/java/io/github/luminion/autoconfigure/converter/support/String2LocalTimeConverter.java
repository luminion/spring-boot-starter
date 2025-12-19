package io.github.luminion.autoconfigure.converter.support;

import io.github.luminion.autoconfigure.converter.DateTimeConverter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * @author luminion
 */
public class String2LocalTimeConverter implements DateTimeConverter<String, LocalTime> {
    private final DateTimeFormatter formatter;

    public String2LocalTimeConverter(String pattern) {
        this.formatter = DateTimeFormatter.ofPattern(pattern);
    }

    @Override
    public LocalTime convert(String source) {
        if (source.isEmpty()) {
            return null;
        }
        return LocalTime.parse(source, formatter);
    }

}
