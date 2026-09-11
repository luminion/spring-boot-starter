package io.github.luminion.velo.webflux;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.trace.TraceContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdWebFluxFilterTests {

    @Test
    void shouldCreateTraceIdExposeItToMdcAndRestoreItAfterCompletion() {
        VeloProperties properties = new VeloProperties();
        TraceIdWebFluxFilter filter = new TraceIdWebFluxFilter(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/users").build());
        AtomicReference<String> traceId = new AtomicReference<>();
        WebFilterChain chain = current -> Mono.deferContextual(contextView -> {
            traceId.set(TraceContext.get(properties.getLog().getTrace().getMdcKey()));
            assertThat(WebFluxContext.traceId(contextView)).isEqualTo(traceId.get());
            return Mono.empty();
        });

        filter.filter(exchange, chain).block();

        assertThat(traceId).isNotNull();
        assertThat(traceId.get()).isNotBlank();
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Trace-Id")).isEqualTo(traceId.get());
        assertThat(TraceContext.get(properties.getLog().getTrace().getMdcKey())).isNull();
    }

    @Test
    void shouldKeepValidIncomingTraceIdAndRejectUnsafeValue() {
        VeloProperties properties = new VeloProperties();
        TraceIdWebFluxFilter filter = new TraceIdWebFluxFilter(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/users").header("X-Trace-Id", "trace-001").build());
        AtomicReference<String> traceId = new AtomicReference<>();

        filter.filter(exchange, current -> Mono.deferContextual(contextView -> {
            traceId.set(WebFluxContext.traceId(contextView));
            return Mono.empty();
        })).block();

        assertThat(traceId.get()).isEqualTo("trace-001");

        MockServerWebExchange unsafeExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/users").header("X-Trace-Id", "bad\ntrace").build());
        filter.filter(unsafeExchange, current -> Mono.empty()).block();

        assertThat(unsafeExchange.getResponse().getHeaders().getFirst("X-Trace-Id"))
                .isNotEqualTo("bad\ntrace");
    }
}
