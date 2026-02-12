package io.github.luminion.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author luminion
 * @since 1.0.0
 */
@ConfigurationProperties("luminion")
@Data
public class Prop {


    /**
     * 日期时间格式
     */
    private DateTimeFormatProperties dateTimeFormat;
    
    @Data
    public class DateTimeFormatProperties {
        /**
         * 时间格式
         */
        private String time = "HH:mm:ss";
        /**
         * 日期格式
         */
        private String date = "yyyy-MM-dd";
        /**
         * 日期时间格式
         */
        private String dateTime = "yyyy-MM-dd HH:mm:ss";
        /**
         * 时区ID
         */
        private String timeZone = "GMT+8";
    }
    
}
