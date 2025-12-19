package io.github.luminion.autoconfigure.aop.exception;

/**
 * 方法限流触发时抛出的自定义运行时异常。
 *
 * @author luminion
 */
public class RateLimitException extends RuntimeException  {

    /**
     * 使用格式化字符串构造一个新的限流异常。
     *
     * @param message 格式化字符串 (e.g., "操作过于频繁, 请稍后重试: %s")
     * @param args    格式化参数
     */
    public RateLimitException(String message, Object... args) {
        super(String.format(message, args));
    }

    /**
     * 使用指定的消息构造一个新的限流异常。
     *
     * @param message 异常消息
     */
    public RateLimitException(String message) {
        super(message);
    }

    /**
     * 使用指定的消息和根本原因构造一个新的限流异常。
     *
     * @param message 异常消息
     * @param cause   根本原因 (用于异常链)
     */
    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 使用指定的根本原因构造一个新的限流异常。
     *
     * @param cause 根本原因 (用于异常链)
     */
    public RateLimitException(Throwable cause) {
        super(cause);
    }
}
