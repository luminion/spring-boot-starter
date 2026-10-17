package io.github.luminion.velo.log.model;

/**
 * 调用日志的输出级别；独立于底层日志框架，OFF 显式关闭对应功能。
 */
public enum LogPolicyLevel {
    /**
     * 最细粒度的跟踪信息。
     */
    TRACE,
    /**
     * 调试信息。
     */
    DEBUG,
    /**
     * 常规运行信息。
     */
    INFO,
    /**
     * 警告信息。
     */
    WARN,
    /**
     * 错误信息。
     */
    ERROR,
    /**
     * 关闭当前日志功能。
     */
    OFF
}
