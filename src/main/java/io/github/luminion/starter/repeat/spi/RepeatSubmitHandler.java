package io.github.luminion.starter.repeat.spi;

/**
 * 防重复提交处理器接口
 * <p>
 * 定义了如何存储和检查重复提交的签名
 *
 * @author luminion
 * @since 1.0.0
 */
@FunctionalInterface
public interface RepeatSubmitHandler {

    /**
     * 检查并标记提交签名
     * <p>
     * 如果签名已存在（表示重复提交），返回true；否则标记签名并返回false
     *
     * @param signature 唯一签名
     * @param timeout   过期时间（秒）
     * @return true表示重复提交，false表示首次提交
     */
    boolean isRepeatSubmit(String signature, int timeout);

}

