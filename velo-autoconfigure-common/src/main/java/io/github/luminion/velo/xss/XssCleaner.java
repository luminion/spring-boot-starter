package io.github.luminion.velo.xss;

/**
 * 定义用于清理或转义输入文本的 XSS 处理器。
 */
public interface XssCleaner {

    /**
     * 按当前清洗策略处理输入文本。
     *
     * @param html 待处理的文本
     * @return 清洗或转义后的文本；实现可以按约定保留 {@code null} 和空字符串
     */
    String clean(String html);
}
