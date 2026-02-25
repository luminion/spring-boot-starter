package io.github.luminion.starter.core.spi;

/**
 * 字符串加密器
 * @author luminion
 * @since 1.0.0
 */
@FunctionalInterface
public interface StringEncryptor {

    /**
     * 加密
     *
     * @param source 来源
     * @return 字符串
     */
    String encrypt(String source);
    
}
