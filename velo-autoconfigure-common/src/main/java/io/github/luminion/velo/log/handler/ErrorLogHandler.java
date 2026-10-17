package io.github.luminion.velo.log.handler;

import io.github.luminion.velo.log.model.InvocationLogEvent;
import io.github.luminion.velo.log.model.InvocationLogFeature;
import io.github.luminion.velo.log.model.InvocationLogPolicy;
import io.github.luminion.velo.log.spi.InvocationLogHandler;
import io.github.luminion.velo.log.spi.LogValueFormatter;
import io.github.luminion.velo.log.support.InvocationLogSupport;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ERROR_LOG 功能的默认内容处理器。
 */
public class ErrorLogHandler implements InvocationLogHandler {
    @Override
    public InvocationLogFeature getFeature() {
        return InvocationLogFeature.ERROR_LOG;
    }

    @Override
    public String render(InvocationLogEvent event, InvocationLogPolicy policy, LogValueFormatter formatter) {
        Throwable error = event.getError();
        if (error == null) {
            return null;
        }
        for (int i = 0; i < 8; i++) {
            boolean wrapper = error instanceof InvocationTargetException
                    || error instanceof UndeclaredThrowableException;
            if (!wrapper || error.getCause() == null || error.getCause() == error) {
                break;
            }
            error = error.getCause();
        }
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("type", error.getClass().getName());
        content.put("message", error.getMessage());
        return InvocationLogSupport.format(content, formatter);
    }
}
