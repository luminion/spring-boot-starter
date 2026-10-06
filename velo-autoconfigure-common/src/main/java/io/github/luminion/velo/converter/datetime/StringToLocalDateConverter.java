package io.github.luminion.velo.converter.datetime;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.format.datetime.standard.DateTimeFormatterFactory;

/**
 * @author luminion
 */
public class StringToLocalDateConverter implements DateTimeConverter<String, LocalDate> {
    private final DateTimeFormatter formatter;

    public StringToLocalDateConverter(String pattern) {
        this.formatter = new DateTimeFormatterFactory(pattern).createDateTimeFormatter();
    }

    @Override
    public LocalDate convert(String source) {
        if (source.isEmpty()) {
            return null;
        }
        return LocalDate.parse(source, formatter);
    }
}
