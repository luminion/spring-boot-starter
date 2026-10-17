package io.github.luminion.velo.log.annotation;

import org.springframework.boot.logging.LogLevel;

/** 日志注解声明的级别；继承状态仅用于策略解析，不传递给日志输出器。 */
public enum LogPolicyLevel {
    INHERIT(null),
    TRACE(LogLevel.TRACE),
    DEBUG(LogLevel.DEBUG),
    INFO(LogLevel.INFO),
    WARN(LogLevel.WARN),
    ERROR(LogLevel.ERROR),
    FATAL(LogLevel.FATAL),
    OFF(LogLevel.OFF);

    private final LogLevel logLevel;

    LogPolicyLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    /** 返回实际输出级别；INHERIT 返回 null，表示继续查找上一级策略。 */
    public LogLevel getLogLevel() {
        return logLevel;
    }
}
