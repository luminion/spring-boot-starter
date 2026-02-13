package io.github.luminion.starter;

import io.github.luminion.starter.xss.XssStrategy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
    private String redisLimitPrefix;
    
    /**
     * XSS处理策略
     */
    private XssStrategy xssStrategy = XssStrategy.RELAXED;
    
    /**
     * 日期时间格式
     */
    private DateTimeFormatProperties dateTimeFormat;
    /**
     * servlet过滤器配置
     */
    private ServletFilterProperties servletFilter;

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

    /**
     * servlet筛选器属性
     *
     * @author luminion
     * @since 1.0.0
     */
    @Data
    public class ServletFilterProperties {
        /**
         * XSS过滤器过滤的URL(Ant样式匹配)
         */
        private Set<String> xssIncludes;
        /**
         * XSS过滤器排除的URL (Ant样式匹配)
         */
        private Set<String> xssExcludes;
   
        /**
         * 防盗链过滤器过滤的URL (Ant样式匹配)
         */
        private Set<String> refererAllowDomains;
    }

}
