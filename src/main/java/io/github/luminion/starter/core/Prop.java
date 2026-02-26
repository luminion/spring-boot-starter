package io.github.luminion.starter.core;

import io.github.luminion.starter.feature.xss.XssStrategy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.logging.LogLevel;

import java.util.Arrays;
import java.util.List;

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
     * 锁校验前缀key
     */
    private String lockPrefix = "lock:";

    /**
     * 日志级别
     */
    private LogLevel logLevel = LogLevel.DEBUG;

    /**
     * XSS处理策略
     */
    private XssStrategy xssStrategy = XssStrategy.RELAXED;

    /**
     * 枚举 编码字段名
     */
    private List<String> enumCodeFields = Arrays.asList("code", "id", "value");

    /**
     * 枚举 描述字段名
     */
    private List<String> enumDescFields = Arrays.asList("desc", "name", "label");

    /**
     * 日期时间格式
     */
    private DateTimeFormatProperties dateTimeFormat;
    /**
     * Web 配置
     */
    private WebProperties web;
    
    @Data
    public class WebProperties {
        /**
         * 允许跨域
         */
        private boolean allowCors;
        
    }
    
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
