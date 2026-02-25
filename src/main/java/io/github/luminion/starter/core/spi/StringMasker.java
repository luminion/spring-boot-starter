package io.github.luminion.starter.core.spi;

/**
 * 字符串脱敏器
 * @author luminion
 * @since 1.0.0
 */
@FunctionalInterface
public interface StringMasker {

    /**
     * 面具
     *
     * @param source 来源
     * @return 字符串
     */
    String mask(String source);
    
}
