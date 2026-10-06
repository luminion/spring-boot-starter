package io.github.luminion.velo.feign;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Feign 同步调用的实际请求上下文；头信息保留为不可修改的快照。
 */
@Getter
public final class FeignInvocationContext {
    private static final ThreadLocal<Deque<FeignInvocationContext>> CURRENT =
            new ThreadLocal<>();
    private Map<String, List<String>> requestHeaders;
    private Map<String, List<String>> responseHeaders;
    @Setter
    private String instanceAddress;

    private FeignInvocationContext() {
    }

    public static FeignInvocationContext open() {
        FeignInvocationContext context = new FeignInvocationContext();
        Deque<FeignInvocationContext> stack = CURRENT.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            CURRENT.set(stack);
        }
        stack.push(context);
        return context;
    }

    /** 只读取已有上下文，无上下文时不在线程上保留空状态。 */
    public static FeignInvocationContext current() {
        Deque<FeignInvocationContext> stack = CURRENT.get();
        if (stack == null || stack.isEmpty()) {
            CURRENT.remove();
            return null;
        }
        return stack.peek();
    }

    public static void close() {
        Deque<FeignInvocationContext> stack = CURRENT.get();
        if (stack == null || stack.isEmpty()) {
            CURRENT.remove();
            return;
        }
        stack.pop();
        if (stack.isEmpty()) {
            CURRENT.remove();
        }
    }

    /**
     * 复制底层请求头，避免调用方后续修改影响日志快照。
     */
    public void captureRequestHeaders(Map<String, ? extends Collection<String>> headers) {
        requestHeaders = copy(headers);
    }

    /**
     * 复制底层响应头，避免调用方后续修改影响日志快照。
     */
    public void captureResponseHeaders(Map<String, ? extends Collection<String>> headers) {
        responseHeaders = copy(headers);
    }

    private Map<String, List<String>> copy(Map<String, ? extends Collection<String>> headers) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        if (headers != null) {
            headers.forEach(
                    (key, value) -> values.put(key, Collections.unmodifiableList(new ArrayList<>(value))));
        }
        return Collections.unmodifiableMap(values);
    }
}
