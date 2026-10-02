package io.github.luminion.velo.log;

/** 日志输出接口；每次写入一条功能记录。 */
@FunctionalInterface
public interface InvocationLogWriter {
  void write(InvocationLogRecord record);

  /** 可选的级别预检，避免被过滤的载荷仍执行序列化；自定义输出器默认接受。 */
  default boolean isEnabled(InvocationLogRecord record) {
    return true;
  }
}
