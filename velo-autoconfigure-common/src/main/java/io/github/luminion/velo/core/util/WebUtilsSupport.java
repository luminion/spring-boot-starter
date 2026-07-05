package io.github.luminion.velo.core.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Servlet-agnostic utility methods extracted from {@code WebUtils} to avoid
 * code duplication between the jakarta and javax variants.
 *
 * @author luminion
 * @since 1.0.0
 */
public final class WebUtilsSupport {

    private static final String[] IP_HEADERS = {
            "x-forwarded-for", "Proxy-Client-IP", "WL-Proxy-Client-IP", "X-Real-IP"
    };

    private WebUtilsSupport() {
    }

    /**
     * Resolves the real client IP from common proxy headers.
     *
     * @param headerResolver function that returns the header value for a given header name
     * @param remoteAddr     the direct connection remote address
     * @return the resolved client IP address
     */
    public static String resolveClientIp(java.util.function.Function<String, String> headerResolver, String remoteAddr) {
        String ip = null;
        for (String header : IP_HEADERS) {
            ip = firstValidIp(headerResolver.apply(header));
            if (ip != null) {
                break;
            }
        }

        // 头部均无有效值时回退到直连地址；X-Forwarded-For 为 "unknown, unknown" 这类全无效串也走此分支
        if (ip == null) {
            ip = firstValidIp(remoteAddr);
        }
        if (ip == null) {
            ip = remoteAddr;
        }

        // IPv6 环回转换放在取出单个 IP 之后，才能覆盖 "0:0:...:1, 10.0.0.1" 这类代理链首段为环回的情况。
        // 同时兼容全展开形（Tomcat getRemoteAddr）与压缩形 "::1"（nginx 经 X-Real-IP/X-Forwarded-For 传入常见）。
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }

    /**
     * 从可能含逗号的头部值中取第一个有效 IP（非空、非 unknown）。
     *
     * @param raw 原始头部值，可能为 null、单个 IP 或逗号分隔的代理链
     * @return 第一个有效 IP；整体为空或全部无效时返回 {@code null}
     */
    private static String firstValidIp(String raw) {
        if (raw == null || raw.isEmpty() || "unknown".equalsIgnoreCase(raw)) {
            return null;
        }
        // indexOf 用 >= 0 覆盖前导逗号（如 ",1.2.3.4"）：首段为空会被下面的有效性判断跳过
        if (raw.indexOf(',') >= 0) {
            for (String subIp : raw.split(",")) {
                String trimmedIp = subIp.trim();
                if (!trimmedIp.isEmpty() && !"unknown".equalsIgnoreCase(trimmedIp)) {
                    return trimmedIp;
                }
            }
            return null;
        }
        return raw.trim();
    }

    /**
     * Builds a Content-Disposition header value for file download, handling
     * URL encoding and RFC 5987 format.
     *
     * @param fileName the full file name (with or without suffix)
     * @param suffix   the file suffix (e.g. {@code .xlsx}), or empty/null if none
     * @return the Content-Disposition header value
     */
    public static String buildContentDisposition(String fileName, String suffix) {
        if (fileName == null || fileName.isEmpty()) {
            fileName = String.valueOf(System.currentTimeMillis());
        }
        if (suffix != null && !suffix.isEmpty() && !fileName.endsWith(suffix)) {
            fileName += suffix;
        }
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        return String.format("attachment; filename=\"%s\"; filename*=utf-8''%s",
                encodedFileName, encodedFileName);
    }
}
