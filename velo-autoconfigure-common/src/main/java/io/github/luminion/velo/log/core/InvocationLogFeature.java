package io.github.luminion.velo.log.core;


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
    SLOW_LOG("slow", InvocationPhase.EXIT),
    REQUEST_HEADERS("requestHeaders", InvocationPhase.ENTRY),
    RESPONSE_HEADERS("responseHeaders", InvocationPhase.EXIT),
    ERROR_LOG("error", InvocationPhase.EXIT);

    private final String label;
    private final InvocationPhase phase;
}
