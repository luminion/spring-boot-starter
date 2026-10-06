package io.github.luminion.velo.converter.datetime;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.format.datetime.standard.DateTimeFormatterFactory;

/**
 * @author luminion
 */
public class StringToLocalDateTimeConverter implements DateTimeConverter<String, LocalDateTime> {
    private final DateTimeFormatter formatter;

    public StringToLocalDateTimeConverter(String pattern) {
        this.formatter = new DateTimeFormatterFactory(pattern).createDateTimeFormatter();
    }

    @Override
    public LocalDateTime convert(String source) {
        if (source.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(source, formatter);
    }

}
