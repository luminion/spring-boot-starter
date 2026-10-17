package io.github.luminion.velo.log.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 每条记录仅对应一个功能；方向只用于文本输出。
 */
@Getter
@RequiredArgsConstructor
public enum InvocationLogFeature {
    ENTRY_ARGS("entryArgs", InvocationPhase.ENTRY),
    EXIT_ARGS("exitArgs", InvocationPhase.EXIT),
    EXIT_RESULT("exitResult", InvocationPhase.EXIT),
    COST_LOG("cost", InvocationPhase.EXIT),
    SLOW_LOG("slow", InvocationPhase.EXIT),
    REQUEST_HEADERS("requestHeaders", InvocationPhase.ENTRY),
    RESPONSE_HEADERS("responseHeaders", InvocationPhase.EXIT),
    ERROR_LOG("error", InvocationPhase.EXIT);

    /**
     * 功能正文在日志中的标签。
     */
    private final String label;
    /**
     * 默认输出方向所属的调用阶段。
     */
    private final InvocationPhase phase;
}
