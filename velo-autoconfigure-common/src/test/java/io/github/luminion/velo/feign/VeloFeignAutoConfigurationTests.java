package io.github.luminion.velo.feign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import feign.RequestTemplate;
import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.log.InvocationLogEngine;
import io.github.luminion.velo.log.InvocationLogFeature;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.VeloLogAutoConfiguration;
import io.github.luminion.velo.log.trace.TraceContext;
import io.github.luminion.velo.log.trace.TraceData;
import io.github.luminion.velo.log.trace.TraceScopeManager;
import io.github.luminion.velo.log.trace.VeloTraceAutoConfiguration;
import io.github.luminion.velo.log.trace.W3cTraceContextResolver;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

class VeloFeignAutoConfigurationTests {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  VeloCoreAutoConfiguration.class,
                  VeloTraceAutoConfiguration.class,
                  VeloLogAutoConfiguration.class,
                  VeloFeignAutoConfiguration.class,
                  VeloFeignTraceAutoConfiguration.class));

  @AfterEach
  void cleanup() {
    MDC.clear();
  }

  @Test
  void createsSharedEngineAndFeignAdapters() {
    runner.run(
        c -> {
          assertThat(c)
              .hasSingleBean(InvocationLogEngine.class)
              .hasSingleBean(FeignLogAspect.class)
              .hasSingleBean(FeignTraceRequestInterceptor.class)
              .hasSingleBean(FeignInvocationCapability.class);
        });
  }

  @Test
  void sourceSwitchKeepsTracePropagation() {
    runner
        .withPropertyValues("velo.log.sources.feign.enabled=false")
        .run(
            c -> {
              assertThat(c)
                  .hasSingleBean(FeignLogAspect.class)
                  .doesNotHaveBean(FeignInvocationCapability.class);
              assertThat(c).hasSingleBean(FeignTraceRequestInterceptor.class);
            });
  }

  @Test
  void disablesAllLogBeans() {
    runner
        .withPropertyValues("velo.log.enabled=false")
        .run(
            c -> {
              assertThat(c)
                  .hasSingleBean(FeignLogAspect.class)
                  .hasSingleBean(FeignTraceRequestInterceptor.class)
                  .doesNotHaveBean(InvocationLogEngine.class);
            });
  }

  @Test
  void missingFeignSkipsAdapters() {
    runner
        .withClassLoader(new FilteredClassLoader("org.springframework.cloud.openfeign"))
        .run(c -> assertThat(c).doesNotHaveBean(FeignLogAspect.class));
  }

  @Test
  void invocationCreatesTraceAndPropagatesWithoutDuplicateHeaders() throws Throwable {
    VeloProperties p = new VeloProperties();
    List<InvocationLogRecord> records = new ArrayList<>();
    List<String> traces = new ArrayList<>();
    FeignLogAspect aspect =
        new FeignLogAspect(
            new InvocationLogEngine(
                p,
                String::valueOf,
                record -> {
                  records.add(record);
                  traces.add(MDC.get("traceId"));
                }),
            new TraceScopeManager(p.getLog().getTrace(), new W3cTraceContextResolver()));
    ProceedingJoinPoint point = point();
    RequestTemplate template = new RequestTemplate();
    template.header("traceparent", "stale");
    template.header("tracestate", "vendor=stale");
    when(point.proceed())
        .thenAnswer(
            call -> {
              new FeignTraceRequestInterceptor(p).apply(template);
              return "done";
            });
    assertThat(aspect.logFeignInvocation(point)).isEqualTo("done");
		assertThat(records).hasSize(2);
    InvocationLogRecord entry = records.get(0);
    assertThat(entry.getFeature()).isEqualTo(InvocationLogFeature.ENTRY_ARGS);
    assertThat(entry.getTarget()).isEqualTo("find() GET /users/{id}");
    assertThat(entry.getContent()).contains("id=1");
    assertThat(traces.get(0)).matches("[0-9a-f]{32}");
    assertThat(template.headers().get("traceparent"))
        .hasSize(1)
        .allMatch(value -> value.startsWith("00-" + traces.get(0) + "-"));
    assertThat(template.headers()).doesNotContainKey("tracestate");
    assertThat(MDC.get("traceId")).isNull();
    assertThat(FeignInvocationContext.current()).isNull();
  }

  @Test
  void headersAreSeparateAllowlistedRecordsAndContextIsRestored() throws Throwable {
    VeloProperties p = new VeloProperties();
    p.getLog().getSources().getFeign().getRequestHeaders().setEnabled(true);
    p.getLog()
        .getSources()
        .getFeign()
        .getRequestHeaders()
        .setAllowlist(java.util.Collections.singletonList("x-request"));
    p.getLog().getSources().getFeign().getResponseHeaders().setEnabled(true);
    p.getLog()
        .getSources()
        .getFeign()
        .getResponseHeaders()
        .setAllowlist(java.util.Collections.singletonList("x-response"));
    List<InvocationLogRecord> records = new ArrayList<>();
    FeignLogAspect aspect =
        new FeignLogAspect(
            new InvocationLogEngine(p, String::valueOf, records::add),
            new TraceScopeManager(p.getLog().getTrace(), new W3cTraceContextResolver()));
    FeignInvocationContext outer = FeignInvocationContext.open();
    try {
      ProceedingJoinPoint point = point();
      when(point.proceed())
          .thenAnswer(
              call -> {
                FeignInvocationContext current = FeignInvocationContext.current();
                current.captureRequestHeaders(
                    java.util.Collections.singletonMap(
                        "X-Request", java.util.Collections.singletonList("yes")));
                current.captureResponseHeaders(
                    java.util.Collections.singletonMap(
                        "X-Response", java.util.Collections.singletonList("ok")));
                throw new IllegalStateException("network");
              });
      assertThatThrownBy(() -> aspect.logFeignInvocation(point))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("network");
      assertThat(records)
          .extracting(InvocationLogRecord::getFeature)
          .containsExactly(
              InvocationLogFeature.ENTRY_ARGS,
              InvocationLogFeature.ERROR_LOG,
              InvocationLogFeature.REQUEST_HEADERS,
              InvocationLogFeature.RESPONSE_HEADERS);
      assertThat(records.get(2).getContent()).contains("X-Request", "yes");
      assertThat(records.get(3).getContent()).contains("X-Response", "ok");
      assertThat(records)
          .extracting(InvocationLogRecord::getTarget)
          .containsOnly("find() GET /users/{id}");
      assertThat(FeignInvocationContext.current()).isSameAs(outer);
    } finally {
      FeignInvocationContext.close();
    }
  }

  @Test
  void interceptorReusesFullContextIncludingSamplingAndState() {
    String id = "4bf92f3577b34da6a3ce929d0e0e4736";
    String parent = "00-" + id + "-00f067aa0ba902b7-01";
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("traceparent", parent);
    headers.put("tracestate", "vendor=preserved");
    TraceData data = new TraceData(id, headers);
    RequestTemplate template = new RequestTemplate();
    template.header("Traceparent", "stale");
    template.header("tracestate", "old=state");
    try (TraceContext.Scope scope = TraceContext.install("traceId", data, true)) {
      new FeignTraceRequestInterceptor(new VeloProperties()).apply(template);
      assertThat(TraceContext.current()).isSameAs(data);
    }
    assertThat(template.headers().get("traceparent")).containsExactly(parent);
    assertThat(template.headers().get("tracestate")).containsExactly("vendor=preserved");
    assertThat(TraceContext.current()).isNull();
    assertThat(MDC.get("traceId")).isNull();
  }

  @Test
  void interceptorWithoutScopeOnlyCreatesOutgoingTrace() {
    RequestTemplate template = new RequestTemplate();
    new FeignTraceRequestInterceptor(new VeloProperties()).apply(template);
    assertThat(template.headers().get("traceparent"))
        .hasSize(1)
        .allMatch(value -> value.matches("00-[0-9a-f]{32}-[0-9a-f]{16}-00"));
    assertThat(MDC.get("traceId")).isNull();
  }

  @Test
  void loggingDisabledFeignAdapterKeepsTraceForWholeCall() {
    runner
        .withPropertyValues("velo.log.enabled=false")
        .run(
            context -> {
              try {
                ProceedingJoinPoint point = point();
                RequestTemplate template = new RequestTemplate();
                List<String> seen = new ArrayList<>();
                when(point.proceed())
                    .thenAnswer(
                        call -> {
                          seen.add(MDC.get("traceId"));
                          context.getBean(FeignTraceRequestInterceptor.class).apply(template);
                          assertThat(MDC.get("traceId")).isEqualTo(seen.get(0));
                          return "done";
                        });
                assertThat(context.getBean(FeignLogAspect.class).logFeignInvocation(point))
                    .isEqualTo("done");
                assertThat(seen.get(0)).matches("[0-9a-f]{32}");
                assertThat(template.headers().get("traceparent"))
                    .allMatch(value -> value.startsWith("00-" + seen.get(0) + "-"));
                assertThat(MDC.get("traceId")).isNull();
              } catch (Throwable error) {
                throw new AssertionError(error);
              }
            });
  }

  @Test
  void propagationSwitchDisablesInterceptorWithoutDisablingLogs() {
    runner
        .withPropertyValues("velo.trace.feign-propagation-enabled=false")
        .run(
            context ->
                assertThat(context)
                    .doesNotHaveBean(FeignTraceRequestInterceptor.class)
                    .hasSingleBean(InvocationLogEngine.class));
  }

  private ProceedingJoinPoint point() throws Exception {
    ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(signature.getMethod()).thenReturn(DemoClient.class.getMethod("find", Long.class));
    when(signature.getDeclaringType()).thenReturn(DemoClient.class);
    when(point.getSignature()).thenReturn(signature);
    when(point.getArgs()).thenReturn(new Object[] {1L});
    return point;
  }

  @FeignClient(name = "demo-client")
  @RequestMapping("/users")
  interface DemoClient {
    @GetMapping("/{id}")
    String find(@PathVariable("id") Long id);
  }
}
