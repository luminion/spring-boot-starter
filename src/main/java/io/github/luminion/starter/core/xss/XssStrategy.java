package io.github.luminion.starter.core.xss;

/**
 * @author luminion
 * @since 1.0.0
 */
public enum XssStrategy {
    /**
     * 不过滤
     */
    NONE,
    /**
     * 转义
     */
    ESCAPE,
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
