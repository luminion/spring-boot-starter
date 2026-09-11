package io.github.luminion.velo.webflux.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 支持 Bean Validation 的 WebFlux 异常处理器。
 *
 * <p>该类仅提供校验异常处理逻辑，不会自动注册为 Spring 组件。应用应在具体实现类上显式添加
 * {@code @RestControllerAdvice}。</p>
 *
 * @param <R> 应用统一响应类型
 * @author luminion
 * @since 1.3.1
 */
@Slf4j
public class VeloValidationWebFluxExceptionHandler<R> extends VeloWebFluxExceptionHandler<R> {

    public VeloValidationWebFluxExceptionHandler(Function<String, R> failed, Function<Throwable, R> error) {
        super(failed, error);
    }

    public VeloValidationWebFluxExceptionHandler(Function<String, R> failed, Function<Throwable, R> error,
            Class<? extends RuntimeException> bizExceptionClass) {
        super(failed, error, bizExceptionClass);
    }

    /**
     * Bean Validation 参数校验异常。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public R handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.debug("[参数校验异常][WebFlux] 校验失败: {}", message);
        return failed.apply(message);
    }

    /**
     * Spring WebFlux 方法参数校验异常（Spring Framework 6.1+）。
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
        log.debug("[参数校验异常][WebFlux][HandlerMethodValidation] 校验失败: {}", message);
        return failed.apply(message);
    }
}
