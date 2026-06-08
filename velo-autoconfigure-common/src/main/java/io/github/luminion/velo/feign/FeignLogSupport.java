package io.github.luminion.velo.feign;

import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * Feign 调用日志公共支持。
 */
final class FeignLogSupport {

    private FeignLogSupport() {
    }

    static String buildInvocationTarget(String clientAddress, Method method, FeignRequestMetadata requestMetadata) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(clientAddress)) {
            builder.append(clientAddress);
        } else {
            builder.append(method.getDeclaringClass().getSimpleName());
        }
        builder.append(' ');
        if (requestMetadata != null && StringUtils.hasText(requestMetadata.getHttpMethod())) {
            builder.append(requestMetadata.getHttpMethod());
        } else {
            builder.append("CALL");
        }
        builder.append(' ');
        if (requestMetadata != null && StringUtils.hasText(requestMetadata.getPath())) {
            builder.append(requestMetadata.getPath());
        } else {
            builder.append(method.getDeclaringClass().getSimpleName()).append('.').append(method.getName());
        }
        return builder.toString();
    }
}
