package io.github.luminion.velo.log.trace;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * W3C 上下文的读取、生成及透传；不采集 span，不改变上游采样决定。
 */
public final class W3cTraceContextResolver implements TraceContextResolver {
    private static final Pattern SIMPLE_KEY = Pattern.compile("[a-z][a-z0-9_*/-]{0,255}");
    private static final Pattern MULTI_KEY =
            Pattern.compile("[a-z0-9][a-z0-9_*/-]{0,240}@[a-z][a-z0-9_*/-]{0,13}");

    @Override
    public TraceData resolve() {
        List<String> parents = CurrentRequestHeaders.values("traceparent");
        String parent = parents.size() == 1 ? trimOws(parents.get(0)) : null;
        String traceId;
        String state = "";
        if (validParent(parent)) {
            traceId = parent.substring(3, 35);
            state = traceState(CurrentRequestHeaders.values("tracestate"));
        } else {
            TraceData existing = TraceContext.current();
            String candidate = existing == null ? null : existing.getTraceId();
            traceId =
                    parents.isEmpty() && nonzeroHex(candidate, 32) ? candidate : TraceContext.createTraceId();
            parent = "00-" + traceId + "-" + TraceContext.createParentId() + "-00";
        }
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("traceparent", parent);
        // 空值用于清除 Feign 重试模板中遗留的厂商上下文。
        headers.put("tracestate", state);
        return new TraceData(traceId, headers);
    }

    private static boolean validParent(String value) {
        if (value == null || value.length() < 55 || value.length() > 8192) {
            return false;
        }
        if (value.charAt(2) != '-'
                || value.charAt(35) != '-'
                || value.charAt(52) != '-'
                || !hex(value.substring(0, 2), 2)
                || value.startsWith("ff")
                || !nonzeroHex(value.substring(3, 35), 32)
                || !nonzeroHex(value.substring(36, 52), 16)
                || !hex(value.substring(53, 55), 2)) {
            return false;
        }
        if (value.startsWith("00")) {
            return value.length() == 55;
        }
        // 保留合法高版本的未知扩展；只转发，不重新编码协议版本。
        if (value.length() > 55 && value.charAt(55) != '-') {
            return false;
        }
        for (int i = 55; i < value.length(); i++) {
            if (value.charAt(i) < 33 || value.charAt(i) > 126) {
                return false;
            }
        }
        return true;
    }

    /**
     * HTTP 可选空白仅为 SP/HTAB，不吞掉换行或其他控制字符。
     */
    private static String trimOws(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && (value.charAt(start) == ' ' || value.charAt(start) == '\t')) {
            start++;
        }
        while (end > start && (value.charAt(end - 1) == ' ' || value.charAt(end - 1) == '\t')) {
            end--;
        }
        return value.substring(start, end);
    }

    private static boolean hex(String value, int length) {
        if (value == null || value.length() != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);
            if (!(c >= '0' && c <= '9') && !(c >= 'a' && c <= 'f')) {
                return false;
            }
        }
        return true;
    }

    private static boolean nonzeroHex(String value, int length) {
        return hex(value, length) && value.chars().anyMatch(c -> c != '0');
    }

    private static String traceState(List<String> fields) {
        List<String> members = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (String field : fields) {
            for (String part : field.split(",", -1)) {
                String member = trimOws(part);
                if (member.isEmpty()) {
                    continue;
                }
                int separator = member.indexOf('=');
                if (separator <= 0 || members.size() == 32) {
                    return "";
                }
                String key = member.substring(0, separator);
                String value = member.substring(separator + 1);
                if (key.length() > 256
                        || !(SIMPLE_KEY.matcher(key).matches() || MULTI_KEY.matcher(key).matches())
                        || !keys.add(key)
                        || value.isEmpty()
                        || value.length() > 256) {
                    return "";
                }
                for (int i = 0; i < value.length(); i++) {
                    char c = value.charAt(i);
                    if (c < 32 || c > 126 || c == '=') {
                        return "";
                    }
                }
                members.add(member);
            }
        }
        // 支持 512 字符，超长时从右侧丢弃低优先级条目。
        String result = String.join(",", members);
        while (result.length() > 512) {
            members.remove(members.size() - 1);
            result = String.join(",", members);
        }
        return result;
    }
}
