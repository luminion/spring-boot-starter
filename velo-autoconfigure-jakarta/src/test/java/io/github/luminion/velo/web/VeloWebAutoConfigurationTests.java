package io.github.luminion.velo.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.core.VeloProperties;
import io.github.luminion.velo.core.spi.RuntimeJsonSerializer;
import io.github.luminion.velo.core.spi.provider.HttpMessageConverterRuntimeJsonSerializer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
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
            ));

    @Test
    void shouldCreateWebMvcConfigurerByDefaultInServletApplication() {
        webContextRunner.run(context -> assertThat(context).hasSingleBean(VeloWebMvcConfigurer.class));
    }

    @Test
    void shouldCreateControllerLogAspectOnlyWhenRequestLoggingEnabled() {
        webContextRunner
                .withPropertyValues("velo.web.request-logging.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(RuntimeJsonSerializer.class);
                    assertThat(context).hasSingleBean(ControllerLogAspect.class);
                    assertThat(ReflectionTestUtils.getField(context.getBean(ControllerLogAspect.class),
                            "runtimeJsonSerializer")).isSameAs(context.getBean(RuntimeJsonSerializer.class));
                });
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
        properties.getWeb().getRequestLogging().setIncludePayload(true);
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
    void shouldDefaultRequestLoggingPayloadLengthToUnlimited() {
        webContextRunner.run(context -> assertThat(context.getBean(io.github.luminion.velo.core.VeloProperties.class)
                .getWeb().getRequestLogging().getMaxPayloadLength()).isZero());
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

    static final class CapturingRuntimeJsonSerializer implements RuntimeJsonSerializer {

        private final List<Object> values = new ArrayList<>();

        @Override
        public String toJson(Object value) {
            values.add(value);
            return "{}";
        }
    }
}
