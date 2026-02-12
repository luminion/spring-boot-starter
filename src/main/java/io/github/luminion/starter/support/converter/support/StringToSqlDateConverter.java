package io.github.luminion.starter.support.converter.support;

import io.github.luminion.starter.support.converter.DateTimeConverter;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @author luminion
 */
public class StringToSqlDateConverter implements DateTimeConverter<String, Date> {
    private final DateTimeFormatter formatter;

    public StringToSqlDateConverter(String pattern) {
        this.formatter = DateTimeFormatter.ofPattern(pattern);
    }

    @Override
    public Date convert(String source) {
        if (source.isEmpty()) {
            return null;
        }
        return Date.valueOf(LocalDate.parse(source, formatter));
    }

}
