package io.github.luminion.velo.log.handler;

import io.github.luminion.velo.log.model.InvocationLogEvent;
import io.github.luminion.velo.log.model.InvocationLogFeature;
import io.github.luminion.velo.log.model.InvocationLogPolicy;
import io.github.luminion.velo.log.spi.InvocationLogHandler;
import io.github.luminion.velo.log.spi.LogValueFormatter;
import io.github.luminion.velo.log.support.InvocationLogSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 请求头和响应头处理器共用采集、脱敏及失败处理，不直接输出日志。
 * <p>
 * 脱敏名单中的头（忽略大小写）保留名称，值输出为 {@value #MASK}，便于确认头是否传递。
 */
abstract class HeadersLogHandler implements InvocationLogHandler {
    static final String MASK = "***";
    private final Set<String> maskedHeaders;

    HeadersLogHandler(Collection<String> maskedHeaders) {
        Set<String> names = new HashSet<>();
        if (maskedHeaders != null) {
            for (String name : maskedHeaders) {
                if (name != null && !name.trim().isEmpty()) {
                    names.add(name.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        this.maskedHeaders = names;
    }

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
                String name = header.getKey();
                boolean masked = name != null && maskedHeaders.contains(name.toLowerCase(Locale.ROOT));
                selected.put(name, masked ? new ArrayList<>(Collections.singletonList(MASK)) : header.getValue());
            }
            return InvocationLogSupport.format(selected, formatter);
        } catch (RuntimeException error) {
            return InvocationLogSupport.SERIALIZATION_FAILED_PAYLOAD;
        }
    }
}
