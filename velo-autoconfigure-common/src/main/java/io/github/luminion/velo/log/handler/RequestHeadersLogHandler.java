package io.github.luminion.velo.log.handler;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.model.InvocationLogFeature;
import java.util.Collection;

/**
 * REQUEST_HEADERS 功能的默认内容处理器。
 */
public class RequestHeadersLogHandler extends HeadersLogHandler {
    /**
     * 使用默认脱敏名单：Authorization、Proxy-Authorization、Cookie、Set-Cookie。
     */
    public RequestHeadersLogHandler() {
        this(new VeloProperties.LogProperties().getMaskedHeaders());
    }

    /**
     * 使用指定的脱敏名单，名称忽略大小写；空集合表示不脱敏。
     *
     * @param maskedHeaders 需要脱敏的请求头名称
     */
    public RequestHeadersLogHandler(Collection<String> maskedHeaders) {
        super(maskedHeaders);
    }

    @Override
    public InvocationLogFeature getFeature() {
        return InvocationLogFeature.REQUEST_HEADERS;
    }
}
