package io.github.luminion.starter.core.exception;

import io.github.luminion.starter.ratelimit.exception.RateLimitException;
import io.github.luminion.starter.repeat.exception.RepeatSubmitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.dao.DataIntegrityViolationException;
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
 * 全局异常处理器基类
 *
 * @author luminion
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class BaseExceptionHandler<R> implements Ordered {
    protected final Function<String, R> failed;
    protected final Function<Throwable, R> error;
    protected final Class<? extends RuntimeException> bizExceptionClass;

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
        log.warn("[参数校验异常][RequestBody] 校验失败: {}", message);
        return failed.apply(message);
    }

    /**
     * Bean Validation 参数绑定异常 (表单/URL参数)
     */
    @ExceptionHandler(BindException.class)
    public R handleBindException(BindException e) {
        BindingResult bindingResult = e.getBindingResult();
        String message = getBindingResultMessage(bindingResult);
        log.warn("[参数绑定异常] 表单/URL参数绑定失败: {}", message);
        return failed.apply(message);
    }


    /**
     * 缺少请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("[缺少必要参数] 参数名: {}", e.getParameterName());
        return failed.apply("缺少必要参数: " + e.getParameterName());
    }

    /**
     * 参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("[参数类型不匹配] 参数名: {}, 期望类型: {}", e.getName(), e.getRequiredType());
        return failed.apply(String.format("参数 '%s' 类型错误", e.getName()));
    }

    /**
     * HTTP 消息不可读 (JSON 解析错误)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("[HTTP消息不可读] 解析失败: {}", e.getLocalizedMessage());
        return failed.apply("请求数据格式错误");
    }

    /**
     * 数据库完整性约束冲突
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public R handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("Duplicate entry")) {
            log.error("[数据库异常] 主键/唯一索引冲突: {}", msg);
            return failed.apply("数据已存在，请勿重复操作");
        }
        if (msg != null && msg.contains("foreign key constraint fails")) {
            log.error("[数据库异常] 外键约束冲突: {}", msg);
            return failed.apply("数据正在被引用，无法删除或修改");
        }
        log.error("[数据库异常] 数据完整性冲突: ", e);
        return failed.apply("数据校验未通过，操作失败");
    }

    /**
     * 不支持的 HTTP 方法
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("[不支持的请求方式] Method: {}", e.getMethod());
        return failed.apply("不支持 " + e.getMethod() + " 请求方式");
    }

    /**
     * 不支持的媒体类型 (Content-Type)
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public R handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("[不支持的媒体类型] Content-Type: {}", e.getContentType());
        return failed.apply("不支持的媒体类型: " + e.getContentType());
    }

    /**
     * 上传文件大小超出限制
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R handleMaxSizeException(MaxUploadSizeExceededException e) {
        log.warn("[文件上传异常] 文件大小超出限制: {}", e.getLocalizedMessage());
        return failed.apply("上传文件超出大小限制");
    }

    /**
     * 非法参数异常
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public R handleIllegalArgumentException(RuntimeException e) {
        log.warn("[逻辑断言失败] 异常信息: {}", e.getMessage());
        return failed.apply(e.getMessage());
    }

    /**
     * 业务异常：限流
     */
    @ExceptionHandler(RateLimitException.class)
    public R handleRateLimitException(RateLimitException e) {
        log.warn("[限流异常] 访问频率过高: {}", e.getMessage());
        return failed.apply(e.getMessage());
    }

    /**
     * 业务异常：防重提交
     */
    @ExceptionHandler(RepeatSubmitException.class)
    public R handleRepeatSubmitException(RepeatSubmitException e) {
        log.warn("[防重提交] 触发重复提交拦截: {}", e.getMessage());
        return failed.apply(e.getMessage());
    }

    /**
     * 兜底：服务器内部异常
     */
    @ExceptionHandler(Exception.class)
    public R handleGlobalException(Exception e) {
        if (bizExceptionClass != null && bizExceptionClass.isAssignableFrom(e.getClass())) {
            log.info("自定义业务异常:{}", e.getMessage());
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
