package io.github.luminion.velo.web.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 支持 Bean Validation 的 Web 异常处理器。
 *
 * @author luminion
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class VeloValidationWebExceptionHandler<R> extends VeloWebExceptionHandler<R> {

    public VeloValidationWebExceptionHandler(Function<String, R> failed, Function<Throwable, R> error) {
        super(failed, error);
    }

    public VeloValidationWebExceptionHandler(Function<String, R> failed, Function<Throwable, R> error,
            Class<? extends RuntimeException> bizExceptionClass) {
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
        log.debug("[参数校验异常][@RequestParam/@PathVariable] 校验失败: {}", message);
        return failed.apply(message);
    }

    /**
     * Spring MVC 方法参数校验异常（Spring Framework 6.1+）。
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public R handleHandlerMethodValidationException(HandlerMethodValidationException e) {
        String parameterMessages = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
        String crossParameterMessages = e.getCrossParameterValidationResults().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
        String message = Stream.of(parameterMessages, crossParameterMessages)
                .filter(value -> value != null && !value.isEmpty())
                .collect(Collectors.joining("; "));
        log.debug("[参数校验异常][HandlerMethodValidation] 校验失败: {}", message);
        return failed.apply(message);
    }
}
