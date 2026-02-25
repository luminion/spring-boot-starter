package io.github.luminion.starter.feature.ratelimit.exception;

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
