package io.github.luminion.starter.core.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author luminion
 * @since 1.0.0
 */
@Slf4j
public class JakartaExceptionHandler<R> extends BaseExceptionHandler<R> {
    
    public JakartaExceptionHandler(Function<String, R> failed, Function<Throwable, R> error) {
        super(failed, error);
    }

    public JakartaExceptionHandler(Function<String, R> failed, Function<Throwable, R> error, Class<? extends RuntimeException> bizExceptionClass) {
        super(failed, error, bizExceptionClass);
    }

    /**
     * Bean Validation 参数校验异常 (@RequestParam/@PathVariable)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public R handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("[参数校验异常][@RequestParam/@PathVariable] 校验失败: {}", message);
        return failed.apply(message);
    }
}
