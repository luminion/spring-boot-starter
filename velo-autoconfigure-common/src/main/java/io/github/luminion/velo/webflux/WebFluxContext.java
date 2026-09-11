package io.github.luminion.velo.webflux;

import org.springframework.web.filter.reactive.ServerWebExchangeContextFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

/**
 * WebFlux 请求上下文的内部约定。
 *
 * <p>WebFlux 不使用 Servlet 的 {@code ThreadLocal} 请求上下文，因此请求和 trace id
 * 通过 Reactor Context 传递。响应式日志组件只从这里读取请求上下文，避免依赖当前线程。</p>
 *
 * @author luminion
 * @since 1.3.1
 */
final class WebFluxContext {

    static final String TRACE_ID_CONTEXT_KEY = WebFluxContext.class.getName() + ".traceId";

    private WebFluxContext() {
    }

    static Context withExchangeAndTrace(Context context, ServerWebExchange exchange, String traceId) {
        return context
                .put(ServerWebExchangeContextFilter.EXCHANGE_CONTEXT_ATTRIBUTE, exchange)
                .put(TRACE_ID_CONTEXT_KEY, traceId);
    }

    static ServerWebExchange exchange(ContextView contextView) {
        if (contextView == null || !contextView.hasKey(ServerWebExchangeContextFilter.EXCHANGE_CONTEXT_ATTRIBUTE)) {
            return null;
        }
        Object value = contextView.get(ServerWebExchangeContextFilter.EXCHANGE_CONTEXT_ATTRIBUTE);
        return value instanceof ServerWebExchange ? (ServerWebExchange) value : null;
    }

    static String traceId(ContextView contextView) {
        if (contextView == null || !contextView.hasKey(TRACE_ID_CONTEXT_KEY)) {
            return null;
        }
        Object value = contextView.get(TRACE_ID_CONTEXT_KEY);
        return value instanceof String ? (String) value : null;
    }
}
