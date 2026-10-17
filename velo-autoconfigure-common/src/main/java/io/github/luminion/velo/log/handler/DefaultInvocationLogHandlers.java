package io.github.luminion.velo.log.handler;

import io.github.luminion.velo.log.spi.InvocationLogHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 创建默认处理器集合；引擎按功能使用业务 Bean 替换其中的一项。
 */
public final class DefaultInvocationLogHandlers {
    private DefaultInvocationLogHandlers() {
    }

    public static List<InvocationLogHandler> create() {
        return new ArrayList<>(Arrays.asList(
                new EntryArgsLogHandler(), new ExitArgsLogHandler(), new ExitResultLogHandler(),
                new CostLogHandler(), new SlowLogHandler(), new ErrorLogHandler(),
                new RequestHeadersLogHandler(), new ResponseHeadersLogHandler()));
    }
}
