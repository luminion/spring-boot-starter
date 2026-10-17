package io.github.luminion.velo.log.handler;

import io.github.luminion.velo.log.model.InvocationLogEvent;
import io.github.luminion.velo.log.model.InvocationLogFeature;
import io.github.luminion.velo.log.model.InvocationLogPolicy;
import io.github.luminion.velo.log.spi.InvocationLogHandler;
import io.github.luminion.velo.log.spi.LogValueFormatter;
import io.github.luminion.velo.log.support.InvocationLogSupport;
import java.util.concurrent.TimeUnit;

/**
 * COST_LOG 功能的默认内容处理器。
 */
public class CostLogHandler implements InvocationLogHandler {
    @Override
    public InvocationLogFeature getFeature() {
        return InvocationLogFeature.COST_LOG;
    }

    @Override
    public String render(InvocationLogEvent event, InvocationLogPolicy policy, LogValueFormatter formatter) {
        return TimeUnit.NANOSECONDS.toMillis(event.getElapsedNanos()) + "ms";
    }
}
