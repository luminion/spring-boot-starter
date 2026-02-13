package io.github.luminion.starter.core.exception;

/**
 * 限流异常
 *
 * @author luminion
 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }

    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }

}
