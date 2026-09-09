package io.github.luminion.velo.converter.datetime;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * @author luminion
 */
public class StringToJavaUtilDateConverter implements DateTimeConverter<String, Date> {
    private final DateTimeFormatter dateTimeFormatter;
    private final DateTimeFormatter dateFormatter;
    private final ZoneId zoneId;

    public StringToJavaUtilDateConverter(String pattern, String zoneId) {
        this(pattern, null, zoneId);
    }

    public StringToJavaUtilDateConverter(String dateTimePattern, String datePattern, String zoneId) {
        this.dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern);
        this.dateFormatter = datePattern == null || datePattern.equals(dateTimePattern)
                ? null : DateTimeFormatter.ofPattern(datePattern);
        this.zoneId = ZoneId.of(zoneId);
    }

    @Override
    public Date convert(String source) {
        if (source.isEmpty()) {
            return null;
        }
        try {
            return Date.from(LocalDateTime.parse(source, dateTimeFormatter).atZone(zoneId).toInstant());
        } catch (DateTimeParseException dateTimeException) {
            if (dateFormatter == null) {
                throw dateTimeException;
            }
            try {
                return Date.from(LocalDate.parse(source, dateFormatter).atStartOfDay(zoneId).toInstant());
            } catch (DateTimeParseException dateException) {
                dateTimeException.addSuppressed(dateException);
                throw dateTimeException;
            }
        }
    }
}
