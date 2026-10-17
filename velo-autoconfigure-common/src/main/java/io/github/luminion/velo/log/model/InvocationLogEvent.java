package io.github.luminion.velo.log.model;

import lombok.Builder;
import lombok.Getter;

/**
 * 功能处理器看到的调用数据；结束事件共享协调器计算的同一段耗时。
 */
@Getter
@Builder
public final class InvocationLogEvent {
    /**
     * 当前入口调用。
     */
    private final LogInvocation invocation;
    /**
     * 已缓存的参数名称。
     */
    private final String[] parameterNames;
    /**
     * 方法是否声明 void 返回值。
     */
    private final boolean voidResult;
    /**
     * 方法原始返回值。
     */
    private final Object result;
    /**
     * 方法原始异常，正常返回时为空。
     */
    private final Throwable error;
    /**
     * 协调器计算的耗时，单位纳秒。
     */
    private final long elapsedNanos;

    public String[] getParameterNames() {
        return parameterNames.clone();
    }
}
