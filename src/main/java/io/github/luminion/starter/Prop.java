package io.github.luminion.starter;

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
     * XSS过滤等级
     */
    private XssCleanLevel xssCleanLevel = XssCleanLevel.RELAXED;
    
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

    /**
     * xss清理策略
     *
     * @author luminion
     * @since 1.0.0
     */
    public enum XssCleanLevel {
        /**
         * 不过滤
         */
        NONE,
        /**
         * 只允许基本的文本格式: b，em，i，strong，u
         */
        SIMPLE_TEXT,
        /**
         * 允许更完整的文本节点: a, b, blockquote, br, cite, code, dd, dl, dt, em, i, li, ol, p, pre, q, small, span, strike, strong, sub, sup, u, ul
         */
        BASIC,
        /**
         * 允许相同文本标记：基本的文本格式以及允许img标签，其中src指向http或https
         */
        BASIC_WITH_IMAGES,
        /**
         * 允许全系列的文本和结构HTML: a，b，blockquote，br，title，cite，code，col，colgroup，dd，div，dl，dt，em，h1，h2，h3，h4，h5，h6，i，img，li，ol，p，pre, q, small, span, strike, strong, sub, sup, table, tbody, td, tfoot, th, thead, tr, u, ul
         */
        RELAXED,
    }
}
