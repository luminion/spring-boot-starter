package io.github.luminion.starter.web.formatter;

import org.springframework.format.Formatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * java.util.Date 格式化器
 *
 * @author luminion
 */
public class JavaUtilDateFormatter implements Formatter<Date> {

    private final String pattern;
    private final String timeZone;

    public JavaUtilDateFormatter(String pattern, String timeZone) {
        this.pattern = pattern;
        this.timeZone = timeZone;
    }

    @Override
    public Date parse(String text, Locale locale) throws ParseException {
        if (text == null || text.isEmpty()) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        if (timeZone != null) {
            sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
        }
        return sdf.parse(text);
    }

    @Override
    public String print(Date object, Locale locale) {
        if (object == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        if (timeZone != null) {
            sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
        }
        return sdf.format(object);
    }
}
