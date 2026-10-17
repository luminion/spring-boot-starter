package io.github.luminion.velo.log.handler;

import io.github.luminion.velo.log.model.InvocationLogFeature;

/**
 * RESPONSE_HEADERS 功能的默认内容处理器。
 */
public class ResponseHeadersLogHandler extends HeadersLogHandler {
    @Override
    public InvocationLogFeature getFeature() {
        return InvocationLogFeature.RESPONSE_HEADERS;
    }
}
