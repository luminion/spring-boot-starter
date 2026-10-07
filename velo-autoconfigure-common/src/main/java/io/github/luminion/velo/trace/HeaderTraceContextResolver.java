package io.github.luminion.velo.trace;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 指定一个请求头即可沿用纯 traceId 协议；缺失、重复或非法时生成新值。
 */
public final class HeaderTraceContextResolver implements TraceContextResolver {
    private final String headerName;

    public HeaderTraceContextResolver(String headerName) {
        if (!TraceData.validHeaderName(headerName)) {
            throw new IllegalArgumentException("Invalid trace header name");
        }
        this.headerName = headerName;
    }

    @Override
    public TraceData resolve() {
        List<String> values = CurrentRequestHeaders.values(headerName);
        String candidate = values.size() == 1 ? values.get(0) : null;
        if (values.isEmpty() && TraceContext.current() != null) {
            candidate = TraceContext.current().getTraceId();
        }
        String traceId = TraceContext.resolveInbound(candidate);
        Map<String, String> headers = Collections.singletonMap(headerName, traceId);
        return new TraceData(traceId, headers);
    }
}
