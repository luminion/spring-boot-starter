package io.github.luminion.velo.converter.datetime;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.springframework.format.datetime.standard.DateTimeFormatterFactory;

/**
 * @author luminion
 */
public class StringToLocalTimeConverter implements DateTimeConverter<String, LocalTime> {
    private final DateTimeFormatter formatter;

    public StringToLocalTimeConverter(String pattern) {
        this.formatter = new DateTimeFormatterFactory(pattern).createDateTimeFormatter();
    }

    @Override
    public LocalTime convert(String source) {
        if (source.isEmpty()) {
            return null;
        }
        return LocalTime.parse(source, formatter);
    }

}
