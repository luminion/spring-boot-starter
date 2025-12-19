package io.github.luminion.autoconfigure.converter.support;

import io.github.luminion.autoconfigure.converter.DateTimeConverter;

import java.sql.Time;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * @author luminion
 */
public class String2SqlTimeConverter implements DateTimeConverter<String, Time> {
    private final DateTimeFormatter formatter;

    public String2SqlTimeConverter(String pattern) {
        this.formatter = DateTimeFormatter.ofPattern(pattern);
    }

    @Override
    public Time convert(String source) {
        if (source.isEmpty()) {
            return null;
        }
        return Time.valueOf(LocalTime.parse(source, formatter));
    }

}
