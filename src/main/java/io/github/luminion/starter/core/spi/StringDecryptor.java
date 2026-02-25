package io.github.luminion.starter.core.spi;

/**
 * 字符串解密器
 * @author luminion
 * @since 1.0.0
 */
@FunctionalInterface
public interface StringDecryptor {

    /**
     * 解密
     *
     * @param source 来源
     * @return 字符串
     */
    String decrypt(String source);
    
}
