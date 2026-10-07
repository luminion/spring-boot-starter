package io.github.luminion.velo.feign;

import java.lang.reflect.Method;

import org.springframework.util.StringUtils;

/**
 * Feign 调用日志公共支持。
 */
final class FeignLogSupport {

    private FeignLogSupport() {
    }

    static String buildInvocationTarget(Method method, FeignRequestMetadata requestMetadata) {
        StringBuilder builder = new StringBuilder(method.getName()).append("()");
        if (requestMetadata != null && StringUtils.hasText(requestMetadata.getHttpMethod())) {
            builder.append(' ').append(requestMetadata.getHttpMethod());
        }
        if (requestMetadata != null && StringUtils.hasText(requestMetadata.getPath())) {
            builder.append(' ').append(requestMetadata.getPath());
        }
        return builder.toString();
    }
}
