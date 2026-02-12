package io.github.luminion.starter.support.repeat.exception;

/**
 * 重复提交异常
 * <p>
 * 当检测到重复提交时抛出此异常
 *
 * @author luminion
 * @since 1.0.0
 */
public class RepeatSubmitException extends RuntimeException {

    public RepeatSubmitException(String message) {
        super(message);
    }

    public RepeatSubmitException(String message, Throwable cause) {
        super(message, cause);
    }

}

