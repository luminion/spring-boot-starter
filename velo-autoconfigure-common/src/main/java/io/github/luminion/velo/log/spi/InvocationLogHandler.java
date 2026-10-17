package io.github.luminion.velo.log.spi;

import io.github.luminion.velo.log.model.InvocationLogEvent;
import io.github.luminion.velo.log.model.InvocationLogFeature;
import io.github.luminion.velo.log.model.InvocationLogPolicy;

/**
 * 单项功能的内容处理器；声明为 Spring Bean 可替换对应默认实现。
 */
public interface InvocationLogHandler {
    InvocationLogFeature getFeature();

    /**
     * 返回功能内容；返回 null 表示本次不生成该项记录，不直接执行日志写入。
     */
    String render(InvocationLogEvent event, InvocationLogPolicy policy, LogValueFormatter formatter);
}
