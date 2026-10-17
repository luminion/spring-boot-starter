package io.github.luminion.velo.log.handler;

import io.github.luminion.velo.log.model.InvocationLogEvent;
import io.github.luminion.velo.log.model.InvocationLogFeature;
import io.github.luminion.velo.log.model.InvocationLogPolicy;
import io.github.luminion.velo.log.spi.InvocationLogHandler;
import io.github.luminion.velo.log.spi.LogValueFormatter;
import io.github.luminion.velo.log.support.InvocationLogSupport;

/**
 * EXIT_RESULT 功能的默认内容处理器。
 */
public class ExitResultLogHandler implements InvocationLogHandler {
    @Override
    public InvocationLogFeature getFeature() {
        return InvocationLogFeature.EXIT_RESULT;
    }

    @Override
    public String render(InvocationLogEvent event, InvocationLogPolicy policy, LogValueFormatter formatter) {
        if (event.getError() != null) {
            return null;
        }
        if (event.isVoidResult()) {
            return InvocationLogSupport.VOID_RESULT;
        }
        return InvocationLogSupport.formatResult(event.getResult(), formatter);
    }
}
