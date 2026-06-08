package io.github.luminion.velo.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogSource;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.trace.TraceContext;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import io.github.luminion.velo.spi.provider.HttpMessageConverterRuntimeJsonSerializer;
import jakarta.servlet.FilterChain;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VeloWebAutoConfigurationTests {

    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    VeloCoreAutoConfiguration.class,
                    VeloWebAutoConfiguration.class
            ))
            .withBean(CapturingInvocationLogWriter.class, CapturingInvocationLogWriter::new);

    @Test
    void shouldCreateWebMvcConfigurerByDefaultInServletApplication() {
        webContextRunner.run(context -> assertThat(context).hasSingleBean(VeloWebMvcConfigurer.class));
    }

    @Test
    void shouldCreateControllerLogAspectAndTraceFilterByDefault() {
        webContextRunner.run(context -> {
            assertThat(context).hasSingleBean(RuntimeJsonSerializer.class);
            assertThat(context).hasSingleBean(ControllerLogAspect.class);
            assertThat(context).hasSingleBean(TraceIdFilter.class);
        });
    }

    @Test
    void shouldSkipControllerLogAspectWhenControllerInvocationLoggingDisabled() {
        webContextRunner
                .withPropertyValues("velo.log.invocation.controller.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ControllerLogAspect.class));
    }

    @Test
    void shouldSkipControllerLogAspectWhenInvocationLoggingDisabled() {
        webContextRunner
                .withPropertyValues("velo.log.invocation.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ControllerLogAspect.class));
    }

    @Test
    void shouldSkipLogBeansWhenLogDisabled() {
        webContextRunner
                .withPropertyValues("velo.log.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ControllerLogAspect.class);
                    assertThat(context).doesNotHaveBean(TraceIdFilter.class);
                });
    }

    @Test
    void shouldSkipTraceIdFilterWhenTraceDisabled() {
        webContextRunner
                .withPropertyValues("velo.log.trace.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(TraceIdFilter.class));
    }

    @Test
    void shouldUseRuntimeMessageConverterForJsonSerialization() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);
        RequestMappingHandlerAdapter handlerAdapter = new RequestMappingHandlerAdapter();
        handlerAdapter.setMessageConverters(Collections.singletonList(new MappingJackson2HttpMessageConverter(objectMapper)));

        webContextRunner
                .withBean(RequestMappingHandlerAdapter.class, () -> handlerAdapter)
                .run(context -> assertThat(context.getBean(RuntimeJsonSerializer.class).toJson(new DemoPayload("tomUser")))
                        .contains("\"user_name\":\"tomUser\""));
    }

    @Test
    void shouldOmitOnlyUnsafeArgumentValueWhenSerializingArgumentMap() {
        RuntimeJsonSerializer serializer = new HttpMessageConverterRuntimeJsonSerializer(Collections.singletonList(
                new MappingJackson2HttpMessageConverter(new ObjectMapper())));
        Map<String, Object> argumentMap = new LinkedHashMap<>();
        argumentMap.put("id", 1L);
        argumentMap.put("request", new MockHttpServletRequest());

        assertThat(serializer.toJson(argumentMap))
                .contains("\"id\":1")
                .contains("\"request\":\"[omitted]\"");
    }

    @Test
    void shouldSerializeControllerArgumentsAsSingleJsonObjectAndResponseBodyOnly() throws Throwable {
        VeloProperties properties = new VeloProperties();
        CapturingRuntimeJsonSerializer serializer = new CapturingRuntimeJsonSerializer();
        CapturingInvocationLogWriter writer = new CapturingInvocationLogWriter();
        ControllerLogAspect aspect = new ControllerLogAspect(properties, serializer, writer);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        DemoPayload body = new DemoPayload("tomUser");

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[] {1L, "Tom"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok(body));
        when(signature.getParameterNames()).thenReturn(new String[] {"id", "name"});

        Object result = aspect.logControllerInvocation(joinPoint);

        assertThat(result).isInstanceOf(ResponseEntity.class);
        assertThat(serializer.values).hasSize(2);
        assertThat(serializer.values.get(0)).isInstanceOf(Map.class);
        Map<?, ?> arguments = (Map<?, ?>) serializer.values.get(0);
        assertThat(arguments.get("id")).isEqualTo(1L);
        assertThat(arguments.get("name")).isEqualTo("Tom");
        assertThat(serializer.values.get(1)).isSameAs(body);
        assertThat(writer.records).hasSize(1);
        assertThat(writer.records.get(0).getSource()).isEqualTo(InvocationLogSource.CONTROLLER);
    }

    @Test
    void shouldMaskNestedSensitiveArgumentsBeforeSerialization() throws Throwable {
        VeloProperties properties = new VeloProperties();
        CapturingRuntimeJsonSerializer serializer = new CapturingRuntimeJsonSerializer();
        CapturingInvocationLogWriter writer = new CapturingInvocationLogWriter();
        ControllerLogAspect aspect = new ControllerLogAspect(properties, serializer, writer);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Map<String, Object> credential = new LinkedHashMap<>();

        credential.put("username", "tom");
        credential.put("password", "123456");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[] {credential});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());
        when(signature.getParameterNames()).thenReturn(new String[] {"request"});

        aspect.logControllerInvocation(joinPoint);

        assertThat(serializer.values).hasSize(2);
        assertThat(serializer.values.get(0)).isInstanceOf(Map.class);
        Map<?, ?> arguments = (Map<?, ?>) serializer.values.get(0);
        Object maskedCredential = arguments.get("request");
        assertThat(maskedCredential).isInstanceOf(Map.class);
        Map<?, ?> maskedCredentialMap = (Map<?, ?>) maskedCredential;
        assertThat(maskedCredentialMap.get("username")).isEqualTo("tom");
        assertThat(maskedCredentialMap.get("password")).isEqualTo("******");
    }

    @Test
    void shouldOmitRawQueryStringFromInvocationTarget() throws Throwable {
        CapturingInvocationLogWriter writer = invokeWithRequest("GET", "/converter/query", request -> {
            request.setQueryString("date1=2010-10-10%2010:10:10&localDate=2010-10-10");
        });

        assertThat(writer.records).hasSize(1);
        assertThat(writer.records.get(0).getTarget()).isEqualTo("127.0.0.1 GET /converter/query");
        assertThat(writer.records.get(0).getTarget()).doesNotContain("date1=");
    }

    @Test
    void shouldPreferControllerMappingTemplateWhenAvailable() throws Throwable {
        CapturingInvocationLogWriter writer = invokeWithRequest("GET", "/converter/query/123", request ->
                request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/converter/query/{id}"));

        assertThat(writer.records).hasSize(1);
        assertThat(writer.records.get(0).getTarget()).isEqualTo("127.0.0.1 GET /converter/query/{id}");
    }

    @Test
    void shouldTruncateLoggedPayloadsUsingConfiguredLimit() throws Throwable {
        VeloProperties properties = new VeloProperties();
        properties.getLog().getInvocation().setMaxPayloadLength(13);
        String longJson = "{\"value\":\"" + String.join("", Collections.nCopies(2100, "a")) + "\"}";
        CapturingInvocationLogWriter writer = invokeWithRequest(properties, value -> longJson);

        assertThat(writer.records).hasSize(1);
        assertThat(writer.records.get(0).getArgs()).isEqualTo("{\"value\":\"...");
        assertThat(writer.records.get(0).getResult()).isEqualTo("{\"value\":\"...");
    }

    @Test
    void shouldAllowUnlimitedLoggedPayloadsWhenConfiguredAsNegativeOne() throws Throwable {
        VeloProperties properties = new VeloProperties();
        properties.getLog().getInvocation().setMaxPayloadLength(-1);
        String longJson = "{\"value\":\"abcdefghijklmnopqrstuvwxyz\"}";
        CapturingInvocationLogWriter writer = invokeWithRequest(properties, value -> longJson);

        assertThat(writer.records).hasSize(1);
        assertThat(writer.records.get(0).getArgs()).isEqualTo(longJson);
        assertThat(writer.records.get(0).getResult()).isEqualTo(longJson);
    }

    @Test
    void shouldLogDashWhenResponseBodyIsNull() throws Throwable {
        VeloProperties properties = new VeloProperties();
        CapturingInvocationLogWriter writer = new CapturingInvocationLogWriter();
        ControllerLogAspect aspect = new ControllerLogAspect(properties, value -> "null", writer);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[] {new Object()});
        when(joinPoint.proceed()).thenReturn(null);
        when(signature.getParameterNames()).thenReturn(new String[] {"arg0"});

        aspect.logControllerInvocation(joinPoint);

        assertThat(writer.records).hasSize(1);
        assertThat(writer.records.get(0).getResult()).isEqualTo("-");
    }

    @Test
    void shouldUseDiscoveredParameterNamesWhenSignatureDoesNotProvideThem() throws Throwable {
        VeloProperties properties = new VeloProperties();
        CapturingRuntimeJsonSerializer serializer = new CapturingRuntimeJsonSerializer();
        CapturingInvocationLogWriter writer = new CapturingInvocationLogWriter();
        ControllerLogAspect aspect = new ControllerLogAspect(properties, serializer, writer);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new NamedParameterController());
        when(joinPoint.getArgs()).thenReturn(new Object[] {1L, "Tom"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());
        when(signature.getDeclaringType()).thenReturn(NamedParameterController.class);
        when(signature.getMethod()).thenReturn(NamedParameterController.class.getDeclaredMethod("find", Long.class, String.class));
        when(signature.getParameterNames()).thenReturn(null);

        aspect.logControllerInvocation(joinPoint);

        assertThat(serializer.values).hasSize(2);
        assertThat(serializer.values.get(0)).isInstanceOf(Map.class);
        Map<?, ?> arguments = (Map<?, ?>) serializer.values.get(0);
        assertThat(arguments.get("id")).isEqualTo(1L);
        assertThat(arguments.get("name")).isEqualTo("Tom");
    }

    @Test
    void shouldCreateTraceIdAndWriteResponseHeader() throws Exception {
        VeloProperties properties = new VeloProperties();
        TraceIdFilter filter = new TraceIdFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        final String[] traceIdInChain = new String[1];
        FilterChain chain = (servletRequest, servletResponse) ->
                traceIdInChain[0] = TraceContext.get(properties.getLog().getTrace().getMdcKey());

        filter.doFilter(request, response, chain);

        assertThat(traceIdInChain[0]).isNotBlank();
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo(traceIdInChain[0]);
        assertThat(TraceContext.get(properties.getLog().getTrace().getMdcKey())).isNull();
    }

    @Test
    void shouldUseIncomingTraceId() throws Exception {
        VeloProperties properties = new VeloProperties();
        TraceIdFilter filter = new TraceIdFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-Trace-Id", "trace-001");
        final String[] traceIdInChain = new String[1];
        FilterChain chain = (servletRequest, servletResponse) ->
                traceIdInChain[0] = TraceContext.get(properties.getLog().getTrace().getMdcKey());

        filter.doFilter(request, response, chain);

        assertThat(traceIdInChain[0]).isEqualTo("trace-001");
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("trace-001");
    }

    private CapturingInvocationLogWriter invokeWithRequest(String method, String uri, RequestCustomizer customizer)
            throws Throwable {
        return invokeWithRequest(new VeloProperties(), value -> "{\"ok\":true}", method, uri, customizer);
    }

    private CapturingInvocationLogWriter invokeWithRequest(VeloProperties properties, RuntimeJsonSerializer serializer)
            throws Throwable {
        return invokeWithRequest(properties, serializer, "POST", "/converter/query", request -> {
        });
    }

    private CapturingInvocationLogWriter invokeWithRequest(VeloProperties properties, RuntimeJsonSerializer serializer,
            String method, String uri, RequestCustomizer customizer) throws Throwable {
        CapturingInvocationLogWriter writer = new CapturingInvocationLogWriter();
        ControllerLogAspect aspect = new ControllerLogAspect(properties, serializer, writer);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);

        request.setRemoteAddr("127.0.0.1");
        customizer.customize(request);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            when(joinPoint.getSignature()).thenReturn(signature);
            when(joinPoint.getArgs()).thenReturn(new Object[] {"Tom"});
            when(joinPoint.proceed()).thenReturn(ResponseEntity.ok(new DemoPayload("tomUser")));
            when(signature.getParameterNames()).thenReturn(new String[] {"name"});

            aspect.logControllerInvocation(joinPoint);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
        return writer;
    }

    interface RequestCustomizer {

        void customize(MockHttpServletRequest request);
    }

    static class DemoPayload {

        private final String userName;

        DemoPayload(String userName) {
            this.userName = userName;
        }

        public String getUserName() {
            return userName;
        }
    }

    static final class NamedParameterController {

        ResponseEntity<Void> find(Long id, String name) {
            return ResponseEntity.ok().build();
        }
    }

    static final class CapturingRuntimeJsonSerializer implements RuntimeJsonSerializer {

        private final List<Object> values = new ArrayList<>();

        @Override
        public String toJson(Object value) {
            values.add(value);
            return "{}";
        }
    }

    static final class CapturingInvocationLogWriter implements InvocationLogWriter {

        private final List<InvocationLogRecord> records = new ArrayList<>();

        @Override
        public void write(InvocationLogRecord record) {
            records.add(record);
        }
    }
}
