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
    private InvocationLogSource source;
    private InvocationLogFeature feature;
    private String target;
    private String loggerName;
    private LogLevel level;
    private String content;
}
