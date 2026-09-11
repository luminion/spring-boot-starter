package io.github.luminion.velo.webflux;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.trace.TraceContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * WebFlux trace id 过滤器。
 *
 * <p>trace id 同时写入响应头、Reactor Context 和当前执行线程的 MDC。Reactor Context
 * 是异步链路中的可靠传递方式；MDC 仅在当前线程执行范围内提供兼容性，跨线程切换时应从
 * Reactor Context 获取 trace id。</p>
 *
 * @author luminion
 * @since 1.3.1
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdWebFluxFilter implements WebFilter {

    private final VeloProperties properties;

    public TraceIdWebFluxFilter(VeloProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        VeloProperties.TraceProperties trace = properties.getLog().getTrace();
        String mdcKey = trace.getMdcKey();
        String previous = TraceContext.get(mdcKey);
        String traceId = TraceContext.resolveInbound(exchange.getRequest().getHeaders().getFirst(trace.getHeaderName()));
        if (trace.isResponseHeaderEnabled()) {
            HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
            responseHeaders.set(trace.getHeaderName(), traceId);
        }

        return Mono.deferContextual(contextView -> {
            String previousAtSubscription = TraceContext.get(mdcKey);
            TraceContext.put(mdcKey, traceId);
            return chain.filter(exchange)
                    .doFinally(signalType -> TraceContext.restore(mdcKey,
                            previousAtSubscription == null ? previous : previousAtSubscription));
        }).contextWrite(context -> WebFluxContext.withExchangeAndTrace(context, exchange, traceId));
    }
}
