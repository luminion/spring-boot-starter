package io.github.luminion.velo.log.handler;

import io.github.luminion.velo.log.model.InvocationLogFeature;

/**
 * REQUEST_HEADERS 功能的默认内容处理器。
 */
public class RequestHeadersLogHandler extends HeadersLogHandler {
    @Override
    public InvocationLogFeature getFeature() {
        return InvocationLogFeature.REQUEST_HEADERS;
    }
}
