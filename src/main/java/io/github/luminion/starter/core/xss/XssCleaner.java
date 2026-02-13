package io.github.luminion.starter.core.xss;

/**
 * @author luminion
 * @since 1.0.0
 */
public interface XssCleaner {

    /**
     * 清理 html
     * @param html html
     * @return 清理后的数据
     */
    String clean(String html);


    /**
     * 是否为xss内容
     *
     * @param html html
     * @return boolean
     */
    boolean isXss(String html);

    
}
