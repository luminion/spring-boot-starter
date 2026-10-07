package io.github.luminion.velo.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.trace.HeaderTraceContextResolver;
import io.github.luminion.velo.trace.TraceContext;
import io.github.luminion.velo.trace.TraceContextResolver;
import io.github.luminion.velo.trace.TraceData;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class TraceResolverFilterTest {
    private static final String ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String PARENT = "00-" + ID + "-00f067aa0ba902b7-01";

    @AfterEach
    void clear() {
        assertThat(TraceContext.current()).isNull();
        MDC.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void standardContextKeepsPropagationHeadersAndRestoresWorker() throws Exception {
        VeloProperties properties = new VeloProperties();
        TraceIdFilter filter = new TraceIdFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("traceparent", PARENT);
        request.addHeader("tracestate", "vendor=value");
        request.addHeader("X-Trace-Id", "legacy-ignored");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put("traceId", "worker");
        filter.doFilter(
                request,
                response,
                (req, res) -> {
                    assertThat(MDC.get("traceId")).isEqualTo(ID);
                    assertThat(TraceContext.current().getPropagationHeaders())
                            .containsEntry("traceparent", PARENT)
                            .containsEntry("tracestate", "vendor=value")
                            .doesNotContainKey("X-Trace-Id");
                    assertThat(RequestContextHolder.getRequestAttributes()).isNull();
                });
        assertThat(response.getHeader("X-Trace-Id")).isNull();
        assertThat(MDC.get("traceId")).isEqualTo("worker");
    }

    @Test
    void noArgumentResolverSeesRequestAndResponseOnlyDuringResolution() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Custom", "custom");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ServletRequestAttributes previous = new ServletRequestAttributes(new MockHttpServletRequest());
        RequestContextHolder.setRequestAttributes(previous);
        TraceContextResolver resolver =
                () -> {
                    ServletRequestAttributes current =
                            (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                    assertThat(current.getRequest()).isSameAs(request);
                    assertThat(current.getResponse()).isSameAs(response);
                    return new HeaderTraceContextResolver("X-Custom").resolve();
                };
        new TraceIdFilter(new VeloProperties(), resolver)
                .doFilter(
                        request,
                        response,
                        (req, res) -> {
                            assertThat(MDC.get("traceId")).isEqualTo("custom");
                            assertThat(RequestContextHolder.getRequestAttributes()).isSameAs(previous);
                        });
        assertThat(response.getHeader("X-Custom")).isNull();
        assertThat(RequestContextHolder.getRequestAttributes()).isSameAs(previous);
    }

    @Test
    void dispatchesReuseCompleteSnapshotAndResolveOnlyOnceAfterFailure() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        TraceData data = new TraceData(ID, Collections.singletonMap("traceparent", PARENT));
        TraceIdFilter filter =
                new TraceIdFilter(
                        new VeloProperties(),
                        () -> {
                            calls.incrementAndGet();
                            return data;
                        });
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThatThrownBy(
                () ->
                        filter.doFilter(
                                request,
                                new MockHttpServletResponse(),
                                (req, res) -> {
                                    assertThat(TraceContext.current()).isSameAs(data);
                                    throw new ServletException("failure");
                                }))
                .isInstanceOf(ServletException.class);
        MDC.put("traceId", "dispatch-worker");
        for (DispatcherType type : new DispatcherType[]{DispatcherType.ASYNC, DispatcherType.ERROR}) {
            request.setDispatcherType(type);
            filter.doFilter(
                    request,
                    new MockHttpServletResponse(),
                    (req, res) -> assertThat(TraceContext.current()).isSameAs(data));
            assertThat(MDC.get("traceId")).isEqualTo("dispatch-worker");
        }
        assertThat(calls).hasValue(1);
    }
}
