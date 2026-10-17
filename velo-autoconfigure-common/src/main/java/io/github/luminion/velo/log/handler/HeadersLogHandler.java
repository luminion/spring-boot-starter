package io.github.luminion.velo.log.handler;

import io.github.luminion.velo.log.model.InvocationLogEvent;
import io.github.luminion.velo.log.model.InvocationLogFeature;
import io.github.luminion.velo.log.model.InvocationLogPolicy;
import io.github.luminion.velo.log.spi.InvocationLogHandler;
import io.github.luminion.velo.log.spi.LogValueFormatter;
import io.github.luminion.velo.log.support.InvocationLogSupport;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 请求头和响应头处理器共用采集、白名单及失败处理，不直接输出日志。
 */
abstract class HeadersLogHandler implements InvocationLogHandler {
    @Override
    public String render(InvocationLogEvent event, InvocationLogPolicy policy, LogValueFormatter formatter) {
        Supplier<Map<String, List<String>>> supplier = getFeature() == InvocationLogFeature.REQUEST_HEADERS
                ? event.getInvocation().getRequestHeaders() : event.getInvocation().getResponseHeaders();
        if (supplier == null) {
            return null;
        }
        try {
            Map<String, List<String>> values = supplier.get();
            if (values == null) {
                return null;
            }
            Map<String, List<String>> selected = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> header : values.entrySet()) {
                boolean allowed = policy.getAllowlist().isEmpty()
                        || policy.getAllowlist().stream().anyMatch(name -> name.equalsIgnoreCase(header.getKey()));
                if (allowed) {
                    selected.put(header.getKey(), header.getValue());
                }
            }
            return InvocationLogSupport.format(selected, formatter);
        } catch (RuntimeException error) {
            return InvocationLogSupport.SERIALIZATION_FAILED_PAYLOAD;
        }
    }
}
