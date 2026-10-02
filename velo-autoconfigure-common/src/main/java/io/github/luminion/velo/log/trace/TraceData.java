package io.github.luminion.velo.log.trace;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 不可变链路快照；只保存标识和协议字段，可安全地随任务传播。
 */
@Getter
@EqualsAndHashCode
@ToString
public final class TraceData {
    private final String traceId;

    /**
     * 下游请求头；空字符串表示删除已有同名头，不发送新值。
     */
    private final Map<String, String> propagationHeaders;

    public TraceData(String traceId, Map<String, String> propagationHeaders) {
        if (!TraceContext.isValid(traceId)) {
            throw new IllegalArgumentException("Invalid traceId");
        }
        this.traceId = traceId;
        this.propagationHeaders = copyHeaders(propagationHeaders);
    }

    private static Map<String, String> copyHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> copy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.forEach(
                (name, value) -> {
                    if (!validHeaderName(name) || value == null || !validHeaderValue(value)) {
                        throw new IllegalArgumentException("Invalid trace propagation header");
                    }
                    if (copy.put(name, value) != null) {
                        throw new IllegalArgumentException("Duplicate trace propagation header");
                    }
                });
        return Collections.unmodifiableMap(copy);
    }

    static boolean validHeaderName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(c >= 'a' && c <= 'z')
                    && !(c >= 'A' && c <= 'Z')
                    && !(c >= '0' && c <= '9')
                    && "!#$%&'*+-.^_`|~".indexOf(c) < 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean validHeaderValue(String value) {
        if (value.length() > 8192) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) < 32 || value.charAt(i) > 126) {
                return false;
            }
        }
        return true;
    }
}
