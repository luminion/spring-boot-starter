package io.github.luminion.starter.core.exception;

/**
 * 幂等异常
 *
 * @author luminion
 */
public class IdempotentException extends RuntimeException {

    public IdempotentException(String message) {
        super(message);
    }

}
