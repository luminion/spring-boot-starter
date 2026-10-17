package io.github.luminion.velo.log.model;

import lombok.Getter;
import lombok.Setter;

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
    private LogPolicyLevel level;
    /**
     * 已格式化的日志内容。
     */
    private String content;
    /**
     * 当前日志产生时的公共信息快照。
     */
    private InvocationLogContext context;
    /**
     * 当前入口格式化后的公共块，不包含外层方括号。
     */
    private String contextContent;
}
