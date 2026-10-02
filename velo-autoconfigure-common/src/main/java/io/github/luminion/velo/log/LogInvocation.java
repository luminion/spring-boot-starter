package io.github.luminion.velo.log;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import lombok.Builder;
import lombok.Getter;

/**
 * 来源适配器提供的调用信息；不依赖 Servlet、Feign 或 AOP 类型。
 */
@Getter
@Builder(toBuilder = true)
public class LogInvocation {
    private final Method method;
    private final Class<?> targetClass;
    private final InvocationLogSource source;
    private final String target;
    private final Object[] arguments;
    private final Supplier<Map<String, List<String>>> requestHeaders;
    private final Supplier<Map<String, List<String>>> responseHeaders;
}
