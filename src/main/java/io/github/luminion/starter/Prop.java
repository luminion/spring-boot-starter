package io.github.luminion.starter;

import io.github.luminion.starter.xss.XssStrategy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.logging.LogLevel;

import java.util.Set;

/**
 * 属性值
 *
 * @author luminion
 * @since 1.0.0
 */
@ConfigurationProperties("luminion")
@Data
public class Prop {


    /**
     * redis限流前缀key
     */
    private String rateLimitPrefix = "rateLimit:";

    /**
     * 幂等校验前缀key
     */
    private String idempotentPrefix = "idempotent:";
    
    /**
     * 日志级别
     */
    private LogLevel logLevel = LogLevel.DEBUG;
    
    /**
     * XSS处理策略
     */
    private XssStrategy xssStrategy = XssStrategy.RELAXED;
    
    /**
     * 日期时间格式
     */
    private DateTimeFormatProperties dateTimeFormat;

    /**
     * 日期时间格式属性
     *
     * @author luminion
     * @since 1.0.0
     */
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
