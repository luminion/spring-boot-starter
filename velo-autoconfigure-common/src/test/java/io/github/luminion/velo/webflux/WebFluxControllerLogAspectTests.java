package io.github.luminion.velo.webflux;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.InvocationPhase;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebFluxControllerLogAspectTests {

    @Test
    void shouldWriteMonoExitLogAfterSubscriptionWithRequestContext() throws Throwable {
        VeloProperties properties = new VeloProperties();
        CapturingWriter writer = new CapturingWriter();
        WebFluxControllerLogAspect aspect = new WebFluxControllerLogAspect(properties,
                value -> "{\"value\":true}", writer);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/users/1").remoteAddress(new java.net.InetSocketAddress("127.0.0.1", 80))
                        .build());
        exchange.getAttributes().put(org.springframework.web.reactive.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                "/users/{id}");

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[] {1L});
        when(signature.getDeclaringType()).thenReturn(DemoController.class);
        when(signature.getReturnType()).thenReturn(Mono.class);
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});
        when(joinPoint.proceed()).thenReturn(Mono.just(new DemoPayload("ok")));

        Object result = aspect.logControllerInvocation(joinPoint);

        assertThat(writer.records).isEmpty();
        ((Mono<?>) result).contextWrite(context -> WebFluxContext.withExchangeAndTrace(context, exchange, "trace-001"))
                .block();

        assertThat(writer.records).hasSize(2);
        assertThat(writer.records.get(0).getPhase()).isEqualTo(InvocationPhase.ENTRY);
        assertThat(writer.records.get(0).getTarget()).isEqualTo("127.0.0.1 GET /users/{id}");
        assertThat(writer.records.get(0).getTraceId()).isEqualTo("trace-001");
        assertThat(writer.records.get(1).getPhase()).isEqualTo(InvocationPhase.EXIT);
        assertThat(writer.records.get(1).isSuccess()).isTrue();
    }

    @Test
    void shouldLogFluxCompletionAsCountWithoutBufferingElements() throws Throwable {
        VeloProperties properties = new VeloProperties();
        CapturingWriter writer = new CapturingWriter();
        WebFluxControllerLogAspect aspect = new WebFluxControllerLogAspect(properties, value -> "unused", writer);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(signature.getDeclaringType()).thenReturn(DemoController.class);
        when(signature.getReturnType()).thenReturn(Flux.class);
        when(signature.getParameterNames()).thenReturn(new String[0]);
        when(joinPoint.proceed()).thenReturn(Flux.just("a", "b", "c"));

        Object result = aspect.logControllerInvocation(joinPoint);
        ((Flux<?>) result).contextWrite(context -> WebFluxContext.withExchangeAndTrace(
                context, MockServerWebExchange.from(MockServerHttpRequest.get("/stream").build()), "trace-002"))
                .blockLast();

        assertThat(writer.records).hasSize(2);
        assertThat(writer.records.get(1).getResult()).isEqualTo("Flux{count=3}");
    }

    static final class DemoController {
    }

    static final class DemoPayload {

        private final String value;

        DemoPayload(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    static final class CapturingWriter implements InvocationLogWriter {

        private final List<InvocationLogRecord> records = new ArrayList<>();

        @Override
        public void write(InvocationLogRecord record) {
            records.add(record);
        }
    }
}
