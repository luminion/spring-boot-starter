package io.github.luminion.starter.core.spi;

/**
 * @author luminion
 * @since 1.0.0
 */
public interface XssHandler {

    /**
     * 清理 html
     * @param html html
     * @return 清理后的数据
     */
    String handle(String html);


    /**
     * 是否为xss内容
     *
     * @param html html
     * @return boolean
     */
    boolean isXss(String html);

    
}
