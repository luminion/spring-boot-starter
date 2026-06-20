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
            ip = headerResolver.apply(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                break;
            }
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = remoteAddr;
        }

        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }

        if (ip != null && ip.indexOf(',') > 0) {
            String[] ips = ip.trim().split(",");
            for (String subIp : ips) {
                String trimmedIp = subIp.trim();
                if (!trimmedIp.isEmpty() && !"unknown".equalsIgnoreCase(trimmedIp)) {
                    ip = trimmedIp;
                    break;
                }
            }
        }
        return ip;
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
