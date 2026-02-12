package io.github.luminion.starter.support.converter.support;

import io.github.luminion.starter.support.converter.DateTimeConverter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * @author luminion
 */
public class StringToLocalTimeConverter implements DateTimeConverter<String, LocalTime> {
    private final DateTimeFormatter formatter;

    public StringToLocalTimeConverter(String pattern) {
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
