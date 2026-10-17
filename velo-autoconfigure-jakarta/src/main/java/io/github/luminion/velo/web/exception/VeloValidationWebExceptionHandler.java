package io.github.luminion.velo.web.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

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
     * 处理 Spring 6.1+ MVC 方法参数或返回值的校验失败；返回值校验失败交给服务器错误转换函数。
     *
     * @param e 方法校验异常
     * @return 校验失败或服务器错误对应的响应体
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public R handleHandlerMethodValidationException(HandlerMethodValidationException e) {
        if (e.isForReturnValue()) {
            return error.apply(e);
        }
        String message = e.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return failed.apply(message);
    }

    /**
     * Bean Validation 参数校验异常 (@RequestParam/@PathVariable)
     *
     * @param e 参数校验异常
     * @return 参数校验失败对应的响应体
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
