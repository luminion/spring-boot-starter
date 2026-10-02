package io.github.luminion.velo.log;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.logging.LogLevel;

/** 单功能调用日志记录；关联字段供自定义输出器使用，不重复打印到正文。 */
@Getter
@Setter
public class InvocationLogRecord {
  private String traceId;
  private String invocationId;
  private InvocationLogSource source;
  private InvocationLogFeature feature;
  private String target;
  private String loggerName;
  private LogLevel level;
  private String payload;
  private long costMs;
  private long thresholdMs;
  private String errorType;
  private String errorMessage;
}
