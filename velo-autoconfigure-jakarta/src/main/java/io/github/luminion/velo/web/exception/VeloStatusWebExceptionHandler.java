package io.github.luminion.velo.web.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 使用 Spring 原生状态码的 MVC 异常处理器。
 */
public class VeloStatusWebExceptionHandler<R> implements Ordered {
    private final Function<String, R> failed;
    private final Function<Throwable, R> error;

    public VeloStatusWebExceptionHandler(Function<String, R> failed, Function<Throwable, R> error) {
        this.failed = Objects.requireNonNull(failed);
        this.error = Objects.requireNonNull(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R> handle(Exception exception) {
        String message = knownFailure(exception);
        R body = message == null ? error.apply(exception) : failed.apply(message);
        return ResponseEntity.status(exception instanceof ErrorResponse
                ? ((ErrorResponse) exception).getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String knownFailure(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException) {
            return ((MethodArgumentNotValidException) exception).getBindingResult().getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining("; "));
        }
        if (exception instanceof BindException) {
            return ((BindException) exception).getBindingResult().getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining("; "));
        }
        if (exception instanceof ConstraintViolationException) {
            return ((ConstraintViolationException) exception).getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage).collect(Collectors.joining("; "));
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
