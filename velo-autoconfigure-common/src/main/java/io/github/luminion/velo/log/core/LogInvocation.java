package io.github.luminion.velo.log.core;


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
    /** 被调用的方法。 */
    private final Method method;
    /** 代理调用对应的目标类。 */
    private final Class<?> targetClass;
    /** 调用日志所属的入口类别。 */
    private final InvocationLogSource source;
    /** 日志中展示的调用目标。 */
    private final String target;
    /** 本次调用的实参数组。 */
    private final Object[] arguments;
    /** 按需提供请求头快照的数据供应器。 */
    private final Supplier<Map<String, List<String>>> requestHeaders;
    /** 按需提供响应头快照的数据供应器。 */
    private final Supplier<Map<String, List<String>>> responseHeaders;
}
