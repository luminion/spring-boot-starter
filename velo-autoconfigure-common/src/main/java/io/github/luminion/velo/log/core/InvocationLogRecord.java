package io.github.luminion.velo.log.core;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.logging.LogLevel;

/**
 * 单功能日志的输出数据；内容已完成对象转换，链路标识由 MDC 提供。
 */
@Getter
@Setter
public class InvocationLogRecord {
    /**
     * 调用日志所属的入口类别。
     */
    private InvocationLogSource source;
    /**
     * 当前日志记录对应的功能。
     */
    private InvocationLogFeature feature;
    /**
     * 当前日志记录展示的调用目标。
     */
    private String target;
    /**
     * 输出日志使用的记录器名称。
     */
    private String loggerName;
    /**
     * 当前日志记录的输出级别。
     */
    private LogLevel level;
    /**
     * 已格式化的日志内容。
     */
    private String content;
}
