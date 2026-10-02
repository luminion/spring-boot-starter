package io.github.luminion.velo.log;

/**
 * 日志对象转换接口；用户提供 Bean 即可替换 Jackson 或 toString 默认实现。
 */
@FunctionalInterface
public interface LogValueFormatter {
    /**
     * 将对象转换为文本；单行处理和长度限制由日志引擎统一执行。
     */
    String format(Object value);
}
