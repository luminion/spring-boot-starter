package io.github.luminion.velo.log.core;


/**
 * 单项日志的方向；不作为正文事件字段输出。
 */
public enum InvocationPhase {
    /**
     * 调用方向，对应 {@code ==>}，包括入参和请求头。
     */
    ENTRY,
    /**
     * 完成方向，对应 {@code <==}，包括结束参数、结果、慢调用、响应头和异常。
     */
    EXIT
}
