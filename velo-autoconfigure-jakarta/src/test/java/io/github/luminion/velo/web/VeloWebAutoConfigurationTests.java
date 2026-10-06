package io.github.luminion.velo.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.log.InvocationLogEngine;
import io.github.luminion.velo.log.InvocationLogFeature;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.LogValueFormatter;
import io.github.luminion.velo.log.VeloLogAutoConfiguration;
import io.github.luminion.velo.log.trace.HeaderTraceContextResolver;
import io.github.luminion.velo.log.trace.VeloTraceAutoConfiguration;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

class VeloWebAutoConfigurationTests {
  private final WebApplicationContextRunner runner =
      new WebApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  VeloCoreAutoConfiguration.class,
                  VeloTraceAutoConfiguration.class,
                  VeloLogAutoConfiguration.class,
                  VeloWebAutoConfiguration.class,
                  VeloWebTraceAutoConfiguration.class));

  @AfterEach
  void clear() {
    MDC.clear();
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void createsSharedEngineAndServletAdapters() {
    runner.run(
        c -> {
          assertThat(
                  c.getBean("traceIdFilterRegistration", FilterRegistrationBean.class)
                      .isAsyncSupported())
              .isTrue();
          assertThat(c)
              .hasSingleBean(VeloWebMvcConfigurer.class)
              .hasSingleBean(ControllerLogAspect.class)
              .hasSingleBean(TraceIdFilter.class)
              .hasSingleBean(InvocationLogEngine.class)
              .hasSingleBean(LogValueFormatter.class);
        });
  }

  @Test
  void controllerSwitchKeepsTraceFilter() {
    runner
        .withPropertyValues("velo.log.sources.controller.enabled=false")
        .run(
            c -> {
              assertThat(c).doesNotHaveBean(ControllerLogAspect.class);
              assertThat(c).hasSingleBean(TraceIdFilter.class);
            });
  }

  @Test
  void logSwitchDisablesLogBeans() {
    runner
        .withPropertyValues("velo.log.enabled=false")
        .run(
            c ->
                assertThat(c)
                    .doesNotHaveBean(ControllerLogAspect.class)
                    .hasSingleBean(TraceIdFilter.class));
  }

  @Test
  void traceSwitchDisablesFilterOnly() {
    runner
        .withPropertyValues("velo.log.trace.enabled=false")
        .run(
            c -> {
              assertThat(c).doesNotHaveBean(TraceIdFilter.class);
              assertThat(c).hasSingleBean(ControllerLogAspect.class);
            });
  }

  @Test
  void webEnhancementSwitchDoesNotDisableIndependentTraceFilter() {
    runner
        .withPropertyValues("velo.web.enabled=false")
        .run(
            context ->
                assertThat(context)
                    .hasSingleBean(TraceIdFilter.class)
                    .doesNotHaveBean(ControllerLogAspect.class));
  }

  @Test
  void acceptsIncomingHeaderAndRestoresExistingMdc() throws Exception {
    VeloProperties p = new VeloProperties();
    TraceIdFilter filter = new TraceIdFilter(p, new HeaderTraceContextResolver("X-Trace-Id"));
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Trace-Id", "incoming");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MDC.put("traceId", "caller");
    MDC.put("user", "alice");
    filter.doFilter(
        request, response, (req, res) -> assertThat(MDC.get("traceId")).isEqualTo("incoming"));
    assertThat(response.getHeader("X-Trace-Id")).isNull();
    assertThat(MDC.get("traceId")).isEqualTo("caller");
    assertThat(MDC.get("user")).isEqualTo("alice");
  }

  @Test
  void freshRequestsDoNotReuseWorkerTrace() throws Exception {
    TraceIdFilter filter = new TraceIdFilter(new VeloProperties());
    MDC.put("traceId", "worker-leftover");
    List<String> ids = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      filter.doFilter(
          new MockHttpServletRequest(),
          new MockHttpServletResponse(),
          (req, res) -> ids.add(MDC.get("traceId")));
    }
    assertThat(ids).hasSize(2).allMatch(id -> id.matches("[0-9a-f]{32}"));
    assertThat(ids.get(0)).isNotEqualTo(ids.get(1));
    assertThat(MDC.get("traceId")).isEqualTo("worker-leftover");
  }

  @Test
  void rejectsUnsafeHeaderAndCleansUpAfterFailure() {
    TraceIdFilter filter = new TraceIdFilter(new VeloProperties());
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("traceparent", "bad trace");
    assertThatThrownBy(
            () ->
                filter.doFilter(
                    request,
                    new MockHttpServletResponse(),
                    (req, res) -> {
                      assertThat(MDC.get("traceId")).matches("[0-9a-f]{32}");
                      throw new ServletException("broken");
                    }))
        .isInstanceOf(ServletException.class)
        .hasMessage("broken");
    assertThat(MDC.get("traceId")).isNull();
  }

  @Test
  void asyncAndErrorDispatchReuseRequestTraceAndRestoreWorker() throws Exception {
    TraceIdFilter filter = new TraceIdFilter(new VeloProperties());
    MockHttpServletRequest request = new MockHttpServletRequest();
    List<String> ids = new ArrayList<>();
    filter.doFilter(
        request, new MockHttpServletResponse(), (req, res) -> ids.add(MDC.get("traceId")));
    MDC.put("traceId", "dispatch-worker");
    for (DispatcherType type : new DispatcherType[] {DispatcherType.ASYNC, DispatcherType.ERROR}) {
      request.setDispatcherType(type);
      filter.doFilter(
          request, new MockHttpServletResponse(), (req, res) -> ids.add(MDC.get("traceId")));
      assertThat(MDC.get("traceId")).isEqualTo("dispatch-worker");
    }
    assertThat(ids).hasSize(3).containsOnly(ids.get(0));
  }

  @Test
  void customKeyKeepsOtherMdcAndDoesNotWriteTraceResponseHeader() throws Exception {
    VeloProperties p = new VeloProperties();
    p.getLog().getTrace().setMdcKey("requestId");
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Request-Id", "custom");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MDC.put("traceId", "unrelated");
    new TraceIdFilter(p, new HeaderTraceContextResolver("X-Request-Id"))
        .doFilter(
            request,
            response,
            (req, res) -> {
              assertThat(MDC.get("requestId")).isEqualTo("custom");
              assertThat(MDC.get("traceId")).isEqualTo("unrelated");
            });
    assertThat(response.getHeader("X-Request-Id")).isNull();
    assertThat(MDC.get("requestId")).isNull();
  }

  @Test
  void controllerUsesMappingTemplateAndDefaultArgsAndResultRecords() throws Throwable {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/1");
    request.setQueryString("secret=hidden");
    request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/users/{id}");
    RequestContextHolder.setRequestAttributes(
        new ServletRequestAttributes(request, new MockHttpServletResponse()));
    List<InvocationLogRecord> records = new ArrayList<>();
    ControllerLogAspect aspect =
        new ControllerLogAspect(
            new InvocationLogEngine(new VeloProperties(), String::valueOf, records::add));
    ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(signature.getMethod()).thenReturn(Endpoint.class.getMethod("find", Long.class));
    when(signature.getDeclaringType()).thenReturn(Endpoint.class);
    when(point.getSignature()).thenReturn(signature);
    when(point.getTarget()).thenReturn(new Endpoint());
    when(point.getArgs()).thenReturn(new Object[] {1L});
    when(point.proceed()).thenReturn("done");
    assertThat(aspect.logControllerInvocation(point)).isEqualTo("done");
    assertThat(records).hasSize(2);
    assertThat(records.get(0).getFeature()).isEqualTo(InvocationLogFeature.ENTRY_ARGS);
    assertThat(records.get(1).getFeature()).isEqualTo(InvocationLogFeature.EXIT_RESULT);
    assertThat(records.get(1).getTarget()).isEqualTo(records.get(0).getTarget());
    assertThat(records.get(1).getContent()).contains("done");
    assertThat(records.get(0).getTarget()).contains("GET /users/{id}").doesNotContain("secret");
    assertThat(records.get(0).getContent()).contains("id=1");
  }

  static class Endpoint {
    public String find(Long id) {
      return "done";
    }
  }
}
