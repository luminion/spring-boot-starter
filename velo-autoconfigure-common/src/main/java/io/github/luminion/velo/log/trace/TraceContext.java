package io.github.luminion.velo.log.trace;

import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Trace id helpers backed by SLF4J MDC.
 */
public final class TraceContext {

    // 入站 traceId 最大长度，超出视为非法。128 足够容纳常见 traceId(如 W3C 32 hex + 业务前缀)
    private static final int MAX_TRACE_ID_LENGTH = 128;

    private TraceContext() {
    }

    public static String get(String mdcKey) {
        if (!StringUtils.hasText(mdcKey)) {
            return null;
        }
        return MDC.get(mdcKey);
    }

    public static void put(String mdcKey, String traceId) {
        if (!StringUtils.hasText(mdcKey) || !StringUtils.hasText(traceId)) {
            return;
        }
        MDC.put(mdcKey, traceId);
    }

    public static void remove(String mdcKey) {
        if (StringUtils.hasText(mdcKey)) {
            MDC.remove(mdcKey);
        }
    }

    /**
     * 恢复 MDC 到指定值：previous 为 null 时清除，否则写回。
     * 用于过滤器/切面执行完毕后还原上游(如 Sleuth/Micrometer Tracing)可能已存在的值，避免误清。
     */
    public static void restore(String mdcKey, String previous) {
        if (!StringUtils.hasText(mdcKey)) {
            return;
        }
        if (previous == null) {
            MDC.remove(mdcKey);
        } else {
            MDC.put(mdcKey, previous);
        }
    }

    /**
     * 校验入站 traceId 是否安全：非空、长度不超上限、仅含 ASCII 字母数字与 - _ .。
     * 拒绝控制字符/换行/空白，防止客户端传入非法值污染或注入日志。
     */
    public static boolean isValid(String traceId) {
        if (traceId == null || traceId.isEmpty() || traceId.length() > MAX_TRACE_ID_LENGTH) {
            return false;
        }
        for (int i = 0; i < traceId.length(); i++) {
            char c = traceId.charAt(i);
            boolean safe = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '-' || c == '_' || c == '.';
            if (!safe) {
                return false;
            }
        }
        return true;
    }

    /**
     * 采纳合法的入站 traceId，非法(含缺失)则重新生成，保证 MDC 中始终是可信值。
     */
    public static String resolveInbound(String candidate) {
        return isValid(candidate) ? candidate : createTraceId();
    }

    public static String createTraceId() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return String.format("%016x%016x", random.nextLong(), random.nextLong());
    }
}
