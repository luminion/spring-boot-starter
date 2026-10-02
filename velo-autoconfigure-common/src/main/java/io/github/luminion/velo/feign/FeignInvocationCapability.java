package io.github.luminion.velo.feign;

import feign.Capability;
import feign.Client;
import feign.Request;
import feign.Response;

import java.io.IOException;
import java.net.URI;

/**
 * 读取 Feign 底层实际请求和响应，不执行额外 DNS 查询。
 */
public class FeignInvocationCapability implements Capability {
    @Override
    public Client enrich(Client client) {
        return new Client() {
            @Override
            public Response execute(Request request, Request.Options options) throws IOException {
                FeignInvocationContext context = FeignInvocationContext.current();
                if (context != null) {
                    context.captureRequestHeaders(request.headers());
                }
                Response response = client.execute(request, options);
                if (context != null && response != null) {
                    context.captureResponseHeaders(response.headers());
                    Request actual = response.request();
                    if (actual != null && !actual.url().equals(request.url())) {
                        try {
                            URI uri = URI.create(actual.url());
                            if (uri.getHost() != null) {
                                context.setInstanceAddress(
                                        uri.getHost() + (uri.getPort() < 0 ? "" : ":" + uri.getPort()));
                            }
                        } catch (IllegalArgumentException ignored) {
                            // 底层客户端未提供可解析的实际地址。
                        }
                    }
                }
                return response;
            }
        };
    }
}
