package io.github.luminion.velo.idempotent.exception;

public class IdempotentException extends RuntimeException {

    public IdempotentException(String message) {
        super(message);
    }
}
