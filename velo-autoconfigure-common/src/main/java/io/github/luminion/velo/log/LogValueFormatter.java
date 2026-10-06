package io.github.luminion.velo.log;

/**
 * 日志对象转换接口；返回完整文本，单行转义由日志引擎处理。
 */
@FunctionalInterface
public interface LogValueFormatter {
    String format(Object value);
}
