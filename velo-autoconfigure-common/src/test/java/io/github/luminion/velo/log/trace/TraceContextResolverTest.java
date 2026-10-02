package io.github.luminion.velo.log.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

class TraceContextResolverTest {
  private static final String ID = "4bf92f3577b34da6a3ce929d0e0e4736";
  private static final String PARENT = "00-" + ID + "-00f067aa0ba902b7-01";

  @AfterEach
  void cleanup() {
    RequestContextHolder.resetRequestAttributes();
    MDC.clear();
    assertThat(TraceContext.current()).isNull();
  }

  @Test
  void standardContextKeepsTraceIdFlagsAndVendorState() {
    request("traceparent", PARENT, "tracestate", "vendor=one,other=two");
    TraceData data = new W3cTraceContextResolver().resolve();
    assertThat(data.getTraceId()).isEqualTo(ID);
    assertThat(data.getPropagationHeaders())
        .containsEntry("traceparent", PARENT)
        .containsEntry("tracestate", "vendor=one,other=two");
    assertThat(MDC.get("traceId")).isNull();
  }

  @Test
  void invalidParentsRestartTraceAndDiscardVendorState() {
    for (String invalid :
        Arrays.asList(
            "00-00000000000000000000000000000000-00f067aa0ba902b7-01",
            "00-" + ID + "-0000000000000000-01",
            PARENT.toUpperCase(),
            "ff" + PARENT.substring(2),
            PARENT + "-extra",
            PARENT + "\n",
            PARENT.substring(0, 53) + "zz",
            "unsafe value")) {
      request("traceparent", invalid, "tracestate", "vendor=old");
      TraceData data = new W3cTraceContextResolver().resolve();
      assertThat(data.getTraceId()).matches("[0-9a-f]{32}").isNotEqualTo(ID);
      assertThat(data.getPropagationHeaders().get("traceparent"))
          .matches("00-" + data.getTraceId() + "-[0-9a-f]{16}-00");
      assertThat(data.getPropagationHeaders()).containsEntry("tracestate", "");
    }
  }

  @Test
  void supportedFutureFormatIsForwardedWithoutDroppingUnknownFields() {
    String future = "01" + PARENT.substring(2) + "-extension";
    request("traceparent", future);
    assertThat(new W3cTraceContextResolver().resolve().getPropagationHeaders())
        .containsEntry("traceparent", future);
  }

  @Test
  void duplicatedTraceparentIsRejected() {
    request(Collections.singletonMap("traceparent", Arrays.asList(PARENT, PARENT)));
    assertThat(new W3cTraceContextResolver().resolve().getTraceId()).isNotEqualTo(ID);
  }

  @Test
  void multipleStateFieldsAreCombinedAndInvalidStateDoesNotInvalidateParent() {
    Map<String, java.util.List<String>> headers = new LinkedHashMap<>();
    headers.put("traceparent", Collections.singletonList(PARENT));
    headers.put("tracestate", Arrays.asList("vendor=one", "other=two"));
    request(headers);
    assertThat(new W3cTraceContextResolver().resolve().getPropagationHeaders())
        .containsEntry("tracestate", "vendor=one,other=two");
    for (String invalid :
        Arrays.asList(
            "vendor=one,vendor=two", "Vendor=bad", "vendor=bad=value", "vendor=bad\r\n")) {
      request("traceparent", PARENT, "tracestate", invalid);
      TraceData data = new W3cTraceContextResolver().resolve();
      assertThat(data.getTraceId()).isEqualTo(ID);
      assertThat(data.getPropagationHeaders()).containsEntry("tracestate", "");
    }
  }

  @Test
  void missingRequestCreatesCompleteNonzeroContextAndDoesNotUseCustomHeader() {
    request("X-Trace-Id", "legacy");
    TraceData first = new W3cTraceContextResolver().resolve();
    RequestContextHolder.resetRequestAttributes();
    TraceData second = new W3cTraceContextResolver().resolve();
    assertThat(first.getTraceId()).matches("[0-9a-f]{32}").isNotEqualTo("legacy");
    assertThat(second.getTraceId()).isNotEqualTo(first.getTraceId());
    assertThat(second.getPropagationHeaders().get("traceparent"))
        .matches("00-[0-9a-f]{32}-[0-9a-f]{16}-00")
        .doesNotContain("-0000000000000000-");
  }

  @Test
  void customHeaderConstructorSupportsOldProtocolAndGeneratesWhenUnsafe() {
    HeaderTraceContextResolver resolver = new HeaderTraceContextResolver("X-Request-Id");
    request("X-Request-Id", "custom-id");
    TraceData data = resolver.resolve();
    assertThat(data.getTraceId()).isEqualTo("custom-id");
    assertThat(data.getPropagationHeaders()).containsEntry("X-Request-Id", "custom-id");
    request("X-Request-Id", "bad value");
    assertThat(resolver.resolve().getTraceId()).matches("[0-9a-f]{32}");
    assertThatThrownBy(() -> new HeaderTraceContextResolver("bad\nheader"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void snapshotCopiesAndProtectsHeadersFromLaterChanges() {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("X-Trace-Id", "first");
    TraceData data = new TraceData("first", headers);
    headers.put("X-Trace-Id", "changed");
    assertThat(data.getPropagationHeaders()).containsEntry("X-Trace-Id", "first");
    assertThatThrownBy(() -> data.getPropagationHeaders().put("X-Test", "value"))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(
            () -> new TraceData("first", Collections.singletonMap("X-Test", "bad\nvalue")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void customThreadLocalResolverRunsOnceAndScopesRestorePreviousContext() {
    ThreadLocal<String> custom = new ThreadLocal<>();
    custom.set("user-context");
    AtomicInteger calls = new AtomicInteger();
    TraceContextResolver resolver =
        () -> {
          calls.incrementAndGet();
          return new TraceData(custom.get(), Collections.emptyMap());
        };
    MDC.put("traceId", "worker");
    try (TraceContext.Scope outer = TraceContext.open("traceId", true, resolver)) {
      TraceData selected = TraceContext.current();
      try (TraceContext.Scope inner = TraceContext.open("traceId", true, resolver)) {
        assertThat(TraceContext.current()).isSameAs(selected);
        assertThat(MDC.get("traceId")).isEqualTo("user-context");
      }
    } finally {
      assertThat(custom.get()).isEqualTo("user-context");
      custom.remove();
    }
    assertThat(calls).hasValue(1);
    assertThat(MDC.get("traceId")).isEqualTo("worker");
  }

  @Test
  void independentRootIgnoresHttpContextAndRestoresItAfterward() {
    request("traceparent", PARENT);
    RequestAttributes request = RequestContextHolder.getRequestAttributes();
    W3cTraceContextResolver resolver = new W3cTraceContextResolver();
    try (TraceContext.Scope outer = TraceContext.open("traceId", true, resolver)) {
      TraceData parent = TraceContext.current();
      try (TraceContext.Scope root = TraceContext.root("traceId", true, resolver)) {
        assertThat(TraceContext.current().getTraceId()).isNotEqualTo(ID);
      }
      assertThat(TraceContext.current()).isSameAs(parent);
    }
    assertThat(RequestContextHolder.getRequestAttributes()).isSameAs(request);
  }

  @Test
  void brokenCustomResolverDoesNotBreakBusinessAndLeavesNoContextBehind() {
    TraceContextResolver resolver =
        () -> {
          throw new IllegalStateException("custom failure");
        };
    MDC.put("traceId", "worker");
    try (TraceContext.Scope scope = TraceContext.open("traceId", true, resolver)) {
      assertThat(TraceContext.current().getTraceId()).matches("[0-9a-f]{32}");
    }
    assertThat(MDC.get("traceId")).isEqualTo("worker");
  }

  private void request(String... pairs) {
    Map<String, java.util.List<String>> headers = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      headers.put(pairs[i], Collections.singletonList(pairs[i + 1]));
    }
    request(headers);
  }

  private void request(Map<String, java.util.List<String>> headers) {
    RequestAttributes attributes = mock(RequestAttributes.class);
    when(attributes.resolveReference(RequestAttributes.REFERENCE_REQUEST))
        .thenReturn(new HeaderRequest(headers));
    RequestContextHolder.setRequestAttributes(attributes);
  }

  /** 用中性对象验证 common 无需绑定 Servlet API。 */
  public static final class HeaderRequest {
    private final Map<String, java.util.List<String>> headers;

    HeaderRequest(Map<String, java.util.List<String>> headers) {
      this.headers = headers;
    }

    public Enumeration<String> getHeaders(String name) {
      return Collections.enumeration(headers.getOrDefault(name, Collections.emptyList()));
    }
  }
}
