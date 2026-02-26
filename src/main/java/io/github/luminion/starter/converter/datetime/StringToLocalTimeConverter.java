package io.github.luminion.starter.converter.datetime;

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
