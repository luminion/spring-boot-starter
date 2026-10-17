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

/** 验证请求过滤器解析入站链路上下文并在派发间复用快照。 */
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
                    assertThat(response.getHeader("X-Trace-Id")).isEqualTo(ID);
                    assertThat(TraceContext.current().getPropagationHeaders())
                            .containsEntry("traceparent", PARENT)
                            .containsEntry("tracestate", "vendor=value")
                            .doesNotContainKey("X-Trace-Id");
                    assertThat(RequestContextHolder.getRequestAttributes()).isNull();
                });
        assertThat(response.getHeaders("X-Trace-Id")).containsExactly(ID);
        assertThat(response.getHeader("traceparent")).isNull();
        assertThat(response.getHeader("tracestate")).isNull();
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
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("custom");
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
        MockHttpServletResponse failedResponse = new MockHttpServletResponse();
        assertThatThrownBy(
                () ->
                        filter.doFilter(
                                request,
                                failedResponse,
                                (req, res) -> {
                                    assertThat(TraceContext.current()).isSameAs(data);
                                    throw new ServletException("failure");
                                }))
                .isInstanceOf(ServletException.class);
        assertThat(failedResponse.getHeader("X-Trace-Id")).isEqualTo(ID);
        MDC.put("traceId", "dispatch-worker");
        for (DispatcherType type : new DispatcherType[]{DispatcherType.ASYNC, DispatcherType.ERROR}) {
            request.setDispatcherType(type);
            MockHttpServletResponse response = new MockHttpServletResponse();
            response.addHeader("X-Trace-Id", "previous");
            filter.doFilter(
                    request,
                    response,
                    (req, res) -> assertThat(TraceContext.current()).isSameAs(data));
            assertThat(response.getHeaders("X-Trace-Id")).containsExactly(ID);
            assertThat(MDC.get("traceId")).isEqualTo("dispatch-worker");
        }
        assertThat(calls).hasValue(1);
    }

    @Test
    void configuredResponseHeaderUsesResolvedValueIndependentlyOfInboundHeadersAndMdc() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.getLog().getTrace().setResponseHeaderName("X-Diagnostic-Id");
        properties.getLog().getTrace().setMdcKey("customMdc");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "inbound-custom");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new TraceIdFilter(properties, new HeaderTraceContextResolver("X-Request-Id"))
                .doFilter(request, response, (req, res) -> {
                    assertThat(response.getHeader("X-Diagnostic-Id")).isEqualTo("inbound-custom");
                    assertThat(MDC.get("customMdc")).isEqualTo("inbound-custom");
                    MDC.put("customMdc", "business-change");
                });
        assertThat(response.getHeader("X-Diagnostic-Id")).isEqualTo("inbound-custom");
        assertThat(response.getHeader("X-Request-Id")).isNull();
        assertThat(response.getHeader("X-Trace-Id")).isNull();
        assertThat(MDC.get("customMdc")).isNull();
    }

    @Test
    void disabledResponseHeaderKeepsTraceContextAndBusinessHeaders() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.getLog().getTrace().setResponseHeaderEnabled(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("traceparent", PARENT);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setHeader("X-Trace-Id", "business");
        new TraceIdFilter(properties).doFilter(request, response, (req, res) ->
                assertThat(MDC.get("traceId")).isEqualTo(ID));
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("business");
    }

    @Test
    void disabledTraceSkipsResolutionAndResponseHeader() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.getLog().getTrace().setEnabled(false);
        MockHttpServletResponse response = new MockHttpServletResponse();
        new TraceIdFilter(properties, () -> {
            throw new AssertionError("Disabled trace must not invoke resolver");
        }).doFilter(new MockHttpServletRequest(), response, (req, res) ->
                assertThat(TraceContext.current()).isNull());
        assertThat(response.getHeader("X-Trace-Id")).isNull();
    }

    @Test
    void missingOrInvalidInboundContextReturnsTheGeneratedTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("traceparent", "invalid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new TraceIdFilter(new VeloProperties()).doFilter(request, response, (req, res) -> {
            String id = MDC.get("traceId");
            assertThat(id).matches("[0-9a-f]{32}").isNotEqualTo("00000000000000000000000000000000");
            assertThat(response.getHeader("X-Trace-Id")).isEqualTo(id);
        });
    }

    @Test
    void nestedErrorDispatchRestoresTheHeaderAfterResponseResetWithoutResolvingAgain() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        TraceIdFilter filter = new TraceIdFilter(new VeloProperties(), () -> {
            calls.incrementAndGet();
            return new TraceData(ID, Collections.emptyMap());
        });
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {
            assertThat(response.getHeader("X-Trace-Id")).isEqualTo(ID);
            response.reset();
            request.setDispatcherType(DispatcherType.ERROR);
            filter.doFilter(request, response, (errorRequest, errorResponse) -> {
                assertThat(response.getHeaders("X-Trace-Id")).containsExactly(ID);
                assertThat(MDC.get("traceId")).isEqualTo(ID);
            });
        });
        assertThat(calls).hasValue(1);
    }

    @Test
    void rejectsInvalidOrW3cProtocolResponseHeaderNames() {
        for (String name : new String[]{"", "bad name", "bad\r\nname", "traceparent", "TraceState"}) {
            VeloProperties properties = new VeloProperties();
            properties.getLog().getTrace().setResponseHeaderName(name);
            assertThatThrownBy(() -> new TraceIdFilter(properties))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("velo.trace.response-header-name");
        }
    }
}
