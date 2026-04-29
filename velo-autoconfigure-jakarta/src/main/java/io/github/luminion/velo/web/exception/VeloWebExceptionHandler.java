package io.github.luminion.velo.web.exception;

import io.github.luminion.velo.idempotent.exception.IdempotentException;
import io.github.luminion.velo.lock.exception.LockException;
import io.github.luminion.velo.ratelimit.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Web 异常处理器基类。
 *
 * @author luminion
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class VeloWebExceptionHandler<R> implements Ordered {

    protected final Function<String, R> failed;
    protected final Function<Throwable, R> error;
    protected final Class<? extends RuntimeException> bizExceptionClass;

    public VeloWebExceptionHandler(Function<String, R> failed, Function<Throwable, R> error) {
        this(failed, error, null);
    }

    private String getBindingResultMessage(BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
    }

    /**
     * Bean Validation 参数校验异常 (@RequestBody)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        String message = getBindingResultMessage(bindingResult);
        log.debug("[参数校验异常][RequestBody] 校验失败: {}", message);
        return failed.apply(message);
    }

    /**
     * 参数绑定异常 (表单/URL参数)
     */
    @ExceptionHandler(BindException.class)
    public R handleBindException(BindException e) {
        BindingResult bindingResult = e.getBindingResult();
        String message = getBindingResultMessage(bindingResult);
        log.debug("[参数绑定异常] 表单/URL参数绑定失败: {}", message);
        return failed.apply(message);
    }

    /**
     * 缺少请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.debug("[缺少必要参数] 参数名: {}", e.getParameterName());
        return failed.apply("缺少必要参数: " + e.getParameterName());
    }

    /**
     * 参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.debug("[参数类型不匹配] 参数名: {}, 期望类型: {}", e.getName(), e.getRequiredType());
        return failed.apply(String.format("参数 '%s' 类型错误", e.getName()));
    }

    /**
     * HTTP 消息不可读 (JSON 解析错误)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.debug("[HTTP消息不可读] 解析失败: {}", e.getLocalizedMessage());
        return failed.apply("请求数据格式错误");
    }

    /**
     * 不支持的 HTTP 方法
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.debug("[不支持的请求方式] Method: {}", e.getMethod());
        return failed.apply("不支持 " + e.getMethod() + " 请求方式");
    }

    /**
     * 不支持的媒体类型 (Content-Type)
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public R handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.debug("[不支持的媒体类型] Content-Type: {}", e.getContentType());
        return failed.apply("不支持的媒体类型: " + e.getContentType());
    }

    /**
     * 上传文件大小超出限制
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R handleMaxSizeException(MaxUploadSizeExceededException e) {
        log.debug("[文件上传异常] 文件大小超出限制: {}", e.getLocalizedMessage());
        return failed.apply("上传文件超出大小限制");
    }

    /**
     * 非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public R handleIllegalArgumentException(IllegalArgumentException e) {
        log.debug("[非法参数] 异常信息: {}", e.getMessage());
        return failed.apply(e.getMessage());
    }

    /**
     * 业务异常：限流
     */
    @ExceptionHandler(RateLimitException.class)
    public R handleRateLimitException(RateLimitException e) {
        log.debug("[限流异常] 访问频率过高: {}", e.getMessage());
        return failed.apply(e.getMessage());
    }

    /**
     * 业务异常：幂等性
     */
    @ExceptionHandler(IdempotentException.class)
    public R handleIdempotentException(IdempotentException e) {
        log.debug("[幂等异常] 触发重复请求拦截: {}", e.getMessage());
        return failed.apply(e.getMessage());
    }

    /**
     * 业务异常：分布式锁
     */
    @ExceptionHandler(LockException.class)
    public R handleLockException(LockException e) {
        log.debug("[锁异常] 获取分布式锁失败: {}", e.getMessage());
        return failed.apply(e.getMessage());
    }

    /**
     * 兜底：服务器内部异常
     */
    @ExceptionHandler(Exception.class)
    public R handleGlobalException(Exception e) {
        if (bizExceptionClass != null && bizExceptionClass.isAssignableFrom(e.getClass())) {
            log.debug("自定义业务异常: {}", e.getMessage());
            return failed.apply(e.getMessage());
        }
        log.error("[系统内部异常] 未捕获的系统级异常: ", e);
        return error.apply(e);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
