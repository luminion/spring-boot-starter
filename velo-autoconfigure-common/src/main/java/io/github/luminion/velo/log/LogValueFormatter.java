package io.github.luminion.velo.log;

import java.io.IOException;
import java.io.Writer;

/**
 * 日志对象转换接口；将内容写入本次调用提供的输出器。
 */
@FunctionalInterface
public interface LogValueFormatter {
    /**
     * 单行处理和字符上限由输出器负责；不关闭输出器，写出异常应向上传递。
     */
    void format(Object value, Writer output) throws IOException;
}
