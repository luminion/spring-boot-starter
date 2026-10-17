package io.github.luminion.velo.web.exception;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;

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

    /**
     * 创建支持 Bean Validation 约束异常的处理器。
     *
     * @param failed 将业务失败消息转换为响应的函数
     * @param error 将未预期异常转换为响应的函数
     */
    public VeloValidationWebExceptionHandler(Function<String, R> failed, Function<Throwable, R> error) {
        super(failed, error);
    }

    /**
     * 处理 {@code @RequestParam} 或 {@code @PathVariable} 参数校验异常。
     *
     * @param e Bean Validation 约束校验异常
     * @return 转换后的失败响应
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
