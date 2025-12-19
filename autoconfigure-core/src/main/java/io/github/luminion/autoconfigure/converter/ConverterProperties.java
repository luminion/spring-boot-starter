package io.github.luminion.autoconfigure.converter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Converter properties
 * @author luminion
 */
@Data
@ConfigurationProperties("turbo.converter")
public class ConverterProperties {
    /**
     * Whether to enable converter autoconfiguration
     */
    private boolean enabled = true;
    /**
     * Whether to enable string to date conversion
     */
    private boolean stringToDate = true;
    /**
     * Whether to enable string to local date time conversion
     */
    private boolean stringToLocalDateTime = true;
    /**
     * Whether to enable string to local date conversion
     */
    private boolean stringToLocalDate = true;
    /**
     * Whether to enable string to local time conversion
     */
    private boolean stringToLocalTime = true;
    /**
     * Whether to enable string to SQL date conversion
     */
    private boolean stringToSqlDate = true;
    /**
     * Whether to enable string to SQL time conversion
     */
    private boolean stringToSqlTime= true;
    /**
     * Whether to enable string to SQL timestamp conversion
     */
    private boolean stringToSqlTimestamp= true;
}