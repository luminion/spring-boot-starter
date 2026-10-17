package io.github.luminion.velo.log.spi;

import io.github.luminion.velo.log.model.InvocationLogRecord;

/**
 * 日志输出接口；每次写入一条功能记录。
 */
@FunctionalInterface
public interface InvocationLogWriter {

    /**
     * 输出一条完整功能记录，可统一定制来源、方法、公共块及功能内容的排列和输出目的地。
     */
    void write(InvocationLogRecord record);

    /**
     * 可选的级别预检，避免被过滤的载荷仍执行序列化；自定义输出器默认接受。
     * 协调器预检时只填充来源、方法、记录器、功能和等级，正文与公共块尚未生成。
     */
    default boolean isEnabled(InvocationLogRecord record) {
        return true;
    }
}
