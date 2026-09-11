package io.github.luminion.velo.webflux.exception;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.function.Function;
import java.util.stream.Collectors;

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
}
