package io.github.luminion.starter.lock.exception;

/**
 * 锁获取异常
 *
 * @author luminion
 * @since 1.0.0
 */
public class LockException extends RuntimeException {
    public LockException(String message) {
        super(message);
    }
}
