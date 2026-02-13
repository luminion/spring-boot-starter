package io.github.luminion.starter.xss;

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
    
}
