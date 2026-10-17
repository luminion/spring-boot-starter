package io.github.luminion.velo.xss;

/**
 * 内置 XSS 清洗策略；具体清理规则由对应的清洗器实现。
 */
public enum XssStrategy {
    /** 不执行内置清洗。 */
    NONE,

    /** 转义 HTML 特殊字符。 */
    ESCAPE,

    /** 仅保留简单文本白名单。 */
    SIMPLE_TEXT,

    /** 使用 Jsoup 的 Basic 白名单。 */
    BASIC,

    /** 使用允许图片的 Jsoup Basic 白名单。 */
    BASIC_WITH_IMAGES,

    /** 使用 Jsoup 的 Relaxed 白名单。 */
    RELAXED
}
