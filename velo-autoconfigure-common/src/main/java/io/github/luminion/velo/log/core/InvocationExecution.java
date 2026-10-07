package io.github.luminion.velo.log.core;


/**
 * 原始调用；日志引擎只执行一次，并保留返回对象和异常。
 */
@FunctionalInterface
public interface InvocationExecution {
    Object proceed() throws Throwable;
}
