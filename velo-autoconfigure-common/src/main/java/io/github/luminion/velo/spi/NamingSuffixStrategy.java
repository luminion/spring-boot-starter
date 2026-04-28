package io.github.luminion.velo.spi;

/**
 * 后缀提供器
 * @author luminion
 * @since 1.0.0
 */
@FunctionalInterface
public interface NamingSuffixStrategy {
    
    /**
     * 提供后缀
     * @return 后缀字符串
     */
    String suffix();

}
