package io.github.luminion.velo.log;

/**
 * 调用日志阶段，用于区分进入记录与退出记录。
 *
 * <p>{@link #ENTRY} 在方法执行前写入，只含入参；
 * {@link #EXIT} 在方法执行后写入，含耗时与返回值或异常。
 * 未设置（null）时以旧单行格式输出，兼容慢日志等场景。</p>
 */
public enum InvocationPhase {

    /** 方法进入时记录，包含入参。对应日志格式：{@code [target] ==> args=...} */
    ENTRY,

    /** 方法退出时记录，包含耗时与返回值或异常。对应日志格式：{@code [target] <== cost=Xms result=...} */
    EXIT
}
