package io.github.luminion.autoconfigure.servlet;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Set;

/**
 * servlet属性
 *
 * @author luminion
 */
@ConfigurationProperties("turbo.servlet.filter")
@Data
public class ServletFilterProperties {
    
    /**
     * Whether to enable servlet autoconfiguration
     */
    private boolean enabled = true;

    /**
     * Comma-separated list of URL patterns to include for XSS filtering (Ant-style matching)
     */
    private Set<String> xssIncludes;
    /**
     * Comma-separated list of URL patterns to exclude from XSS filtering (Ant-style matching)
     */
    private Set<String> xssExcludes;
    /**
     * XSS protection level
     */
    private SanitizerType xssSanitizer = SanitizerType.RELAXED;
    
    /**
     * Comma-separated list of allowed domains for referer validation
     */
    private Set<String> refererAllowDomains;
    /**
     * Whether to enable repeatable read filter
     */
    private Boolean repeatable = true;



    public enum SanitizerType {
        /**
         * Disallows any HTML tags
         */
        NONE,
        /**
         * Allows only basic text formatting: b, em, i, strong, u
         */
        SIMPLE_TEXT,
        /**
         * Allows more complete text nodes: a, b, blockquote, br, cite, code, dd, dl, dt, em, i, li, ol, p, pre, q, small, span, strike, strong, sub, sup, u, ul with appropriate attributes
         */
        BASIC,
        /**
         * Allows same text markup as basic plus img tags with appropriate attributes where src points to http or https
         */
        BASIC_WITH_IMAGES,
        /**
         * Allows full range of text and structural HTML: a, b, blockquote, br, caption, cite, code, col, colgroup, dd, div, dl, dt, em, h1, h2, h3, h4, h5, h6, i, img, li, ol, p, pre, q, small, span, strike, strong, sub, sup, table, tbody, td, tfoot, th, thead, tr, u, ul
         */
        RELAXED,
    }

}