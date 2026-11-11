package io.github.luminion.velo.log.handler;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.spi.InvocationLogHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 创建默认处理器集合；引擎按功能使用业务 Bean 替换其中的一项。
 */
public final class DefaultInvocationLogHandlers {
    private DefaultInvocationLogHandlers() {
    }

    public static List<InvocationLogHandler> create() {
        return create(new VeloProperties.LogProperties().getMaskedHeaders());
    }

    /**
     * 创建默认处理器集合，头日志使用指定的脱敏名单。
     *
     * @param maskedHeaders 需要脱敏的头名称，忽略大小写
     * @return 可变的默认处理器集合
     */
    public static List<InvocationLogHandler> create(Collection<String> maskedHeaders) {
        return new ArrayList<>(Arrays.asList(
                new EntryArgsLogHandler(), new ExitArgsLogHandler(), new ExitResultLogHandler(),
                new CostLogHandler(), new SlowLogHandler(), new ErrorLogHandler(),
                new RequestHeadersLogHandler(maskedHeaders), new ResponseHeadersLogHandler(maskedHeaders)));
    }
}
