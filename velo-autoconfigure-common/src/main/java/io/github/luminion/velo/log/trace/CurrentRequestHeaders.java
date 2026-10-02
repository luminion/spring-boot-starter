package io.github.luminion.velo.log.trace;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * 延迟访问 Spring 请求上下文，避免 common 绑定 javax/jakarta Servlet 类型。
 */
final class CurrentRequestHeaders {
    private static final boolean SPRING_WEB_PRESENT =
            ClassUtils.isPresent(
                    "org.springframework.web.context.request.RequestContextHolder",
                    CurrentRequestHeaders.class.getClassLoader());

    private CurrentRequestHeaders() {
    }

    static List<String> values(String name) {
        return SPRING_WEB_PRESENT ? SpringAccess.values(name) : Collections.emptyList();
    }

    static <T> T withoutRequest(Supplier<T> action) {
        return SPRING_WEB_PRESENT ? SpringAccess.withoutRequest(action) : action.get();
    }

    /**
     * 仅在 spring-web 存在时加载；非 Web 应用仍可以生成链路。
     */
    private static final class SpringAccess {
        @SuppressWarnings("unchecked")
        private static List<String> values(String name) {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            Object request =
                    attributes == null
                            ? null
                            : attributes.resolveReference(RequestAttributes.REFERENCE_REQUEST);
            if (request == null) {
                return Collections.emptyList();
            }
            Method method = ReflectionUtils.findMethod(request.getClass(), "getHeaders", String.class);
            if (method == null) {
                return Collections.emptyList();
            }
            ReflectionUtils.makeAccessible(method);
            Object values = ReflectionUtils.invokeMethod(method, request, name);
            return values instanceof Enumeration
                    ? Collections.list((Enumeration<String>) values)
                    : Collections.emptyList();
        }

        private static <T> T withoutRequest(Supplier<T> action) {
            RequestAttributes previous = RequestContextHolder.getRequestAttributes();
            try {
                RequestContextHolder.resetRequestAttributes();
                return action.get();
            } finally {
                if (previous == null) {
                    RequestContextHolder.resetRequestAttributes();
                } else {
                    RequestContextHolder.setRequestAttributes(previous);
                }
            }
        }
    }
}
