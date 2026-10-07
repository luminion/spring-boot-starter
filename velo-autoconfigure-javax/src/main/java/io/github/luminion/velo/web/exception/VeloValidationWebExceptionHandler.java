package io.github.luminion.velo.web.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 支持 Bean Validation 的 Web 异常处理器。
 *
 * <p>通过 {@code @Bean} 注册并提供响应转换函数即可生效；不会被组件扫描自动实例化。</p>
 *
 * @author luminion
 * @since 1.0.0
 */
@Slf4j
public class VeloValidationWebExceptionHandler<R> extends VeloWebExceptionHandler<R> {

    public VeloValidationWebExceptionHandler(Function<String, R> failed, Function<Throwable, R> error) {
        super(failed, error);
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
}
