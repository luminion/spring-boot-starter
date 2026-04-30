package io.github.luminion.velo.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import io.github.luminion.velo.spi.provider.HttpMessageConverterRuntimeJsonSerializer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class VeloWebAutoConfigurationTests {

    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    VeloCoreAutoConfiguration.class,
                    VeloWebAutoConfiguration.class
            ));

    @Test
    void shouldCreateWebMvcConfigurerByDefaultInServletApplication() {
        webContextRunner.run(context -> assertThat(context).hasSingleBean(VeloWebMvcConfigurer.class));
    }

    @Test
    void shouldCreateControllerLogAspectByDefault() {
        webContextRunner.run(context -> {
            assertThat(context).hasSingleBean(RuntimeJsonSerializer.class);
            assertThat(context).hasSingleBean(ControllerLogAspect.class);
            assertThat(ReflectionTestUtils.getField(context.getBean(ControllerLogAspect.class),
                    "runtimeJsonSerializer")).isSameAs(context.getBean(RuntimeJsonSerializer.class));
        });
    }

    @Test
    void shouldSkipControllerLogAspectWhenRequestLoggingDisabled() {
        webContextRunner
                .withPropertyValues("velo.web.request-logging-enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ControllerLogAspect.class));
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
        ControllerLogAspect aspect = new ControllerLogAspect(properties, serializer);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        DemoPayload body = new DemoPayload("tomUser");

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[] {1L, "Tom"});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok(body));
        when(signature.getDeclaringType()).thenReturn(DemoController.class);
        when(signature.getName()).thenReturn("find");
        when(signature.getParameterNames()).thenReturn(new String[] {"id", "name"});

        Object result = aspect.logControllerInvocation(joinPoint);

        assertThat(result).isInstanceOf(ResponseEntity.class);
        assertThat(serializer.values).hasSize(2);
        assertThat(serializer.values.get(0)).isInstanceOf(Map.class);
        Map<?, ?> arguments = (Map<?, ?>) serializer.values.get(0);
        assertThat(arguments.get("id")).isEqualTo(1L);
        assertThat(arguments.get("name")).isEqualTo("Tom");
        assertThat(serializer.values.get(1)).isSameAs(body);
    }

    @Test
    void shouldOmitRawQueryStringFromLogs(CapturedOutput output) throws Throwable {
        VeloProperties properties = new VeloProperties();
        ControllerLogAspect aspect = new ControllerLogAspect(properties, value -> "{\"ok\":true}");
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/converter/query");

        request.setRemoteAddr("127.0.0.1");
        request.setQueryString("date1=2010-10-10%2010:10:10&localDate=2010-10-10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            when(joinPoint.getSignature()).thenReturn(signature);
            when(joinPoint.getArgs()).thenReturn(new Object[] {"Tom"});
            when(joinPoint.proceed()).thenReturn(ResponseEntity.ok(new DemoPayload("tomUser")));
            when(signature.getDeclaringType()).thenReturn(DemoController.class);
            when(signature.getParameterNames()).thenReturn(new String[] {"name"});

            aspect.logControllerInvocation(joinPoint);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }

        assertThat(output.getOut()).contains("[127.0.0.1 GET /converter/query] ==> args: {\"ok\":true}");
        assertThat(output.getOut()).contains("[127.0.0.1 GET /converter/query] <== cost:");
        assertThat(output.getOut()).doesNotContain("query=");
        assertThat(output.getOut()).doesNotContain("date1=2010-10-10%2010:10:10");
    }

    @Test
    void shouldTruncateLoggedPayloadsWithFixedLimit(CapturedOutput output) throws Throwable {
        VeloProperties properties = new VeloProperties();
        String longJson = "{\"value\":\"" + String.join("", Collections.nCopies(2100, "a")) + "\"}";
        ControllerLogAspect aspect = new ControllerLogAspect(properties, value -> longJson);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/converter/query");

        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            when(joinPoint.getSignature()).thenReturn(signature);
            when(joinPoint.getArgs()).thenReturn(new Object[] {"Tom"});
            when(joinPoint.proceed()).thenReturn(ResponseEntity.ok(new DemoPayload("tomUser")));
            when(signature.getDeclaringType()).thenReturn(DemoController.class);
            when(signature.getParameterNames()).thenReturn(new String[] {"name"});

            aspect.logControllerInvocation(joinPoint);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }

        assertThat(output.getOut()).contains("[127.0.0.1 POST /converter/query] ==> args: {\"value\":\"");
        assertThat(output.getOut()).contains("[127.0.0.1 POST /converter/query] <== cost:");
        assertThat(output.getOut()).contains("resp: {\"value\":\"");
        assertThat(output.getOut()).contains("...");
        assertThat(output.getOut()).doesNotContain("query=");
    }

    @Test
    void shouldLogDashWhenResponseBodyIsNull(CapturedOutput output) throws Throwable {
        VeloProperties properties = new VeloProperties();
        ControllerLogAspect aspect = new ControllerLogAspect(properties, value -> "null");
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/fast-excel/export");

        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            when(joinPoint.getSignature()).thenReturn(signature);
            when(joinPoint.getArgs()).thenReturn(new Object[] {new Object()});
            when(joinPoint.proceed()).thenReturn(null);
            when(signature.getDeclaringType()).thenReturn(DemoController.class);
            when(signature.getParameterNames()).thenReturn(new String[] {"arg0"});

            aspect.logControllerInvocation(joinPoint);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }

        assertThat(output.getOut()).contains("[127.0.0.1 GET /fast-excel/export] <== cost:");
        assertThat(output.getOut()).contains("resp: -");
        assertThat(output.getOut()).doesNotContain("resp: null");
    }

    @Test
    void shouldUseDiscoveredParameterNamesWhenSignatureDoesNotProvideThem() throws Throwable {
        VeloProperties properties = new VeloProperties();
        CapturingRuntimeJsonSerializer serializer = new CapturingRuntimeJsonSerializer();
        ControllerLogAspect aspect = new ControllerLogAspect(properties, serializer);
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

    static class DemoPayload {

        private final String userName;

        DemoPayload(String userName) {
            this.userName = userName;
        }

        public String getUserName() {
            return userName;
        }
    }

    static final class DemoController {
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
}
