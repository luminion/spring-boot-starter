package io.github.luminion.velo.converter.datetime;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * 支持主格式和备用格式解析的 DateFormat。
 *
 * <p>格式化输出始终使用主格式，解析时先尝试主格式，再尝试备用格式，
 * 用于给 Jackson 提供不限制用户显式 {@code @JsonFormat} 的默认能力。</p>
 *
 * @author luminion
 */
public class FlexibleDateFormat extends DateFormat {

    private static final long serialVersionUID = 1L;

    private final String primaryPattern;
    private final String fallbackPattern;

    public FlexibleDateFormat(String primaryPattern, String fallbackPattern, TimeZone timeZone) {
        this.primaryPattern = primaryPattern;
        this.fallbackPattern = fallbackPattern;
        TimeZone actualTimeZone = timeZone == null ? TimeZone.getDefault() : timeZone;
        Calendar calendar = Calendar.getInstance(actualTimeZone);
        setCalendar(calendar);
        setNumberFormat(NumberFormat.getIntegerInstance());
    }

    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        return createFormatter(primaryPattern).format(date, toAppendTo, fieldPosition);
    }

    @Override
    public Date parse(String source, ParsePosition position) {
        int startIndex = position.getIndex();
        Date parsed = tryParse(source, startIndex, primaryPattern, position);
        if (parsed != null) {
            return parsed;
        }

        int primaryErrorIndex = position.getErrorIndex();
        position.setIndex(startIndex);
        position.setErrorIndex(-1);

        if (fallbackPattern != null && !fallbackPattern.equals(primaryPattern)) {
            parsed = tryParse(source, startIndex, fallbackPattern, position);
            if (parsed != null) {
                return parsed;
            }
        }

        position.setIndex(startIndex);
        position.setErrorIndex(primaryErrorIndex >= 0 ? primaryErrorIndex : startIndex);
        return null;
    }

    private Date tryParse(String source, int startIndex, String pattern, ParsePosition targetPosition) {
        if (source == null || pattern == null || pattern.isEmpty()) {
            return null;
        }

        ParsePosition candidatePosition = new ParsePosition(startIndex);
        Date parsed = createFormatter(pattern).parse(source, candidatePosition);
        if (parsed != null && candidatePosition.getIndex() == source.length()) {
            targetPosition.setIndex(candidatePosition.getIndex());
            targetPosition.setErrorIndex(-1);
            return parsed;
        }
        if (candidatePosition.getErrorIndex() >= 0) {
            targetPosition.setErrorIndex(candidatePosition.getErrorIndex());
        }
        return null;
    }

    private SimpleDateFormat createFormatter(String pattern) {
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        formatter.setTimeZone(getTimeZone());
        formatter.setLenient(isLenient());
        return formatter;
    }
}
