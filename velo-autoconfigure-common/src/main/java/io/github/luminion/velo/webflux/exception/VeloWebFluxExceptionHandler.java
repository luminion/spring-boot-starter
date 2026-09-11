package io.github.luminion.velo.webflux.exception;

import io.github.luminion.velo.idempotent.exception.IdempotentException;
import io.github.luminion.velo.lock.exception.LockException;
import io.github.luminion.velo.ratelimit.exception.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * WebFlux 异常处理器基类。
 *
 * <p>该类只提供异常处理逻辑，不会自动注册为 Spring 组件。应用应在具体实现类上显式添加
 * {@code @RestControllerAdvice}，并通过构造函数提供响应转换函数。</p>
 *
 * @param <R> 应用统一响应类型
 * @author luminion
 * @since 1.3.1
 */
@Slf4j
public class VeloWebFluxExceptionHandler<R> implements Ordered {

    protected final Function<String, R> failed;

    protected final Function<Throwable, R> error;

    protected final Class<? extends RuntimeException> bizExceptionClass;

    public VeloWebFluxExceptionHandler(Function<String, R> failed, Function<Throwable, R> error) {
        this(failed, error, null);
    }

    public VeloWebFluxExceptionHandler(Function<String, R> failed, Function<Throwable, R> error,
            Class<? extends RuntimeException> bizExceptionClass) {
        this.failed = failed;
        this.error = error;
        this.bizExceptionClass = bizExceptionClass;
    }

    /**
     * WebFlux 请求体或模型校验异常。
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public R handleWebExchangeBindException(WebExchangeBindException e) {
        String message = e.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.debug("[Validation][WebFlux] validation failed: {}", message);
        return failed.apply(message);
    }

    /**
     * WebFlux 请求输入异常，包括 JSON 解码失败和类型转换失败。
     */
    @ExceptionHandler(ServerWebInputException.class)
    public R handleServerWebInputException(ServerWebInputException e) {
        log.debug("[WebFluxInput] request input failed: {}", e.getReason());
        return failed.apply(Objects.toString(e.getReason(), "请求数据格式错误"));
    }

    /**
     * 缺少或不支持的请求方法。
     */
    @ExceptionHandler(MethodNotAllowedException.class)
    public R handleMethodNotAllowedException(MethodNotAllowedException e) {
        log.debug("[MethodNotAllowed] method: {}", e.getHttpMethod());
        return failed.apply("不支持 " + e.getHttpMethod() + " 请求方式");
    }

    /**
     * 不支持的请求媒体类型。
     */
    @ExceptionHandler(UnsupportedMediaTypeStatusException.class)
    public R handleUnsupportedMediaTypeStatusException(UnsupportedMediaTypeStatusException e) {
        MediaType contentType = e.getContentType();
        log.debug("[MediaTypeNotSupported] content-type: {}", contentType);
        return failed.apply("不支持的媒体类型: " + Objects.toString(contentType, "未知"));
    }

    /**
     * 请求体超过 WebFlux 编解码缓冲区限制。
     */
    @ExceptionHandler(DataBufferLimitException.class)
    public R handleDataBufferLimitException(DataBufferLimitException e) {
        log.debug("[DataBufferLimit] request body exceeds configured limit: {}", e.getMessage());
        return failed.apply("请求数据超过大小限制");
    }

    /**
     * 业务异常：限流。
     */
    @ExceptionHandler(RateLimitException.class)
    public R handleRateLimitException(RateLimitException e) {
        if (e.getKey() != null) {
            log.warn("[RateLimit] key={}, limit={}/{}ms, message={}",
                    e.getKey(), e.getPermits(), e.getWindow(), e.getMessage());
        } else {
            log.warn("[RateLimit] {}", e.getMessage());
        }
        return failed.apply(e.getMessage());
    }

    /**
     * 业务异常：幂等性。
     */
    @ExceptionHandler(IdempotentException.class)
    public R handleIdempotentException(IdempotentException e) {
        if (e.getKey() != null) {
            log.warn("[Idempotent] key={}, window={}ms, message={}",
                    e.getKey(), e.getTtl(), e.getMessage());
        } else {
            log.warn("[Idempotent] {}", e.getMessage());
        }
        return failed.apply(e.getMessage());
    }

    /**
     * 业务异常：分布式锁。
     */
    @ExceptionHandler(LockException.class)
    public R handleLockException(LockException e) {
        if (e.getKey() != null) {
            log.warn("[Lock] key={}, waitTimeout={}ms, lease={}ms, message={}",
                    e.getKey(), e.getWaitTimeout(), e.getLease(), e.getMessage());
        } else {
            log.warn("[Lock] {}", e.getMessage());
        }
        return failed.apply(e.getMessage());
    }

    /**
     * 兜底：服务器内部异常。
     */
    @ExceptionHandler(Exception.class)
    public R handleGlobalException(Exception e) {
        if (bizExceptionClass != null && bizExceptionClass.isAssignableFrom(e.getClass())) {
            log.debug("[BizException] message: {}", e.getMessage());
            return failed.apply(Objects.toString(e.getMessage(), "系统繁忙，请稍后再试"));
        }
        log.error("[InternalError] uncaught system exception: ", e);
        return error.apply(e);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
