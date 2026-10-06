package io.github.luminion.velo.log;

import io.github.luminion.velo.util.InvocationUtils;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.ReflectionUtils;

/** 调用适配、对象载荷和安全日志工具。 */
public final class InvocationLogSupport {
  private static final int MAX_SANITIZED_VALUES = 256;
  private static final Logger LOGGER = LoggerFactory.getLogger(InvocationLogSupport.class);
  public static final String DISABLED_PAYLOAD = "disabled";
  public static final String SERIALIZATION_FAILED_PAYLOAD = "serialization-failed";
  public static final String VOID_RESULT = "void";

  private InvocationLogSupport() {}

  public static LogInvocation invocation(
      ProceedingJoinPoint point, InvocationLogSource source, String target) {
    MethodSignature signature = (MethodSignature) point.getSignature();
    Class<?> type =
        point.getTarget() == null
            ? signature.getDeclaringType()
            : AopUtils.getTargetClass(point.getTarget());
    return LogInvocation.builder()
        .method(signature.getMethod())
        .targetClass(type)
        .source(source)
        .target(target)
        .arguments(point.getArgs())
        .build();
  }

  static Map<String, Object> arguments(String[] names, Object[] args) {
    Map<String, Object> values = new LinkedHashMap<>();
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        values.put(i < names.length ? names[i] : "arg" + i, args[i]);
      }
    }
    return values;
  }

  public static String format(Object value, LogValueFormatter formatter, int maxLength) {
    if (maxLength == 0) {
      return DISABLED_PAYLOAD;
    }
    LogPayloadWriter output = new LogPayloadWriter(maxLength);
    SanitizationBudget budget = new SanitizationBudget();
    try {
      Object safeValue = sanitize(value, new IdentityHashMap<>(), 0, budget);
      formatter.format(safeValue, output);
      return output.content(budget.omitted);
    } catch (IOException | RuntimeException error) {
      if (output.isLimitReached(error)) {
        return output.content(budget.omitted);
      }
      // 不用 toString 回退，避免绕过 Jackson 的字段忽略规则。
      return limit(SERIALIZATION_FAILED_PAYLOAD, maxLength);
    }
  }

  /** HTTP 返回包装只记录 body，保持 Jackson 字段规则，不序列化包装中的技术状态。 */
  static String formatResult(Object value, LogValueFormatter formatter, int maxLength) {
    if (maxLength == 0) {
      return DISABLED_PAYLOAD;
    }
    try {
      if (value != null) {
        for (Class<?> type = value.getClass(); type != null; type = type.getSuperclass()) {
          // 使用名称识别，保持非 Web 应用不依赖 Spring HTTP 类型。
          if ("org.springframework.http.HttpEntity".equals(type.getName())) {
            value =
                ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(type, "getBody"), value);
            break;
          }
        }
      }
      return format(value, formatter, maxLength);
    } catch (RuntimeException error) {
      return limit(SERIALIZATION_FAILED_PAYLOAD, maxLength);
    }
  }

  public static boolean enabled(InvocationLogWriter writer, InvocationLogRecord record) {
    try {
      return writer.isEnabled(record);
    } catch (RuntimeException error) {
      return false;
    }
  }

  public static void safeWrite(InvocationLogWriter writer, InvocationLogRecord record) {
    try {
      writer.write(record);
    } catch (RuntimeException error) {
      LOGGER.warn("Invocation log writer failed for {}", singleLine(record.getTarget()));
    }
  }

  /** 保留 DTO，避免改写 Jackson 字段语义；流、Servlet 等技术对象不读取内容。 */
  private static Object sanitize(
      Object value, IdentityHashMap<Object, Boolean> visited, int depth, SanitizationBudget budget) {
    budget.remaining--;
    if (value == null) {
      return null;
    }
    if (depth > 32) {
      return "[omitted]";
    }
    if (visited.containsKey(value)) {
      return "[circular]";
    }
    if (value instanceof Map<?, ?>
        || value instanceof Collection<?>
        || value.getClass().isArray()) {
      visited.put(value, Boolean.TRUE);
      try {
        if (value instanceof Map<?, ?>) {
          Map<Object, Object> result = new LinkedHashMap<>();
          for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
            if (budget.remaining == 0) {
              budget.omitted = true;
              break;
            }
            result.put(entry.getKey(), sanitize(entry.getValue(), visited, depth + 1, budget));
          }
          return result;
        }
        Collection<Object> result = new ArrayList<>();
        if (value instanceof Collection<?>) {
          Iterator<?> items = ((Collection<?>) value).iterator();
          while (items.hasNext()) {
            if (budget.remaining == 0) {
              budget.omitted = true;
              result.add("[omitted]");
              break;
            }
            result.add(sanitize(items.next(), visited, depth + 1, budget));
          }
        } else {
          for (int i = 0; i < Array.getLength(value); i++) {
            if (budget.remaining == 0) {
              budget.omitted = true;
              result.add("[omitted]");
              break;
            }
            result.add(sanitize(Array.get(value, i), visited, depth + 1, budget));
          }
        }
        return result;
      } finally {
        visited.remove(value);
      }
    }
    return InvocationUtils.isLoggableValue(value) ? value : "[omitted]";
  }

  private static final class SanitizationBudget {
    private int remaining = MAX_SANITIZED_VALUES;
    private boolean omitted;
  }

  public static String singleLine(String value) {
    if (value == null) {
      return "";
    }
    StringBuilder result = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '\n') {
        result.append("\\n");
      } else if (c == '\r') {
        result.append("\\r");
      } else if (c == '\t') {
        result.append("\\t");
      } else if (c < 0x20 || c == '\u2028' || c == '\u2029') {
        result.append(String.format("\\u%04x", (int) c));
      } else {
        result.append(c);
      }
    }
    return result.toString();
  }

  public static String quote(String value) {
    String escaped =
        singleLine(value == null ? null : value.replace("\\", "\\\\").replace("\"", "\\\""));
    return "\"" + escaped + "\"";
  }

  static String limit(String value, int maxLength) {
    if (maxLength < 0 || value.length() <= maxLength) {
      return value;
    }
    int end = maxLength <= 3 ? maxLength : maxLength - 3;
    if (end > 0 && Character.isHighSurrogate(value.charAt(end - 1))) {
      end--;
    }
    return value.substring(0, end) + (maxLength > 3 ? "..." : "");
  }

  public static boolean exceedsSlowThresholdNanos(long elapsedNanos, long threshold) {
    return elapsedNanos >= TimeUnit.MILLISECONDS.toNanos(threshold);
  }
}
