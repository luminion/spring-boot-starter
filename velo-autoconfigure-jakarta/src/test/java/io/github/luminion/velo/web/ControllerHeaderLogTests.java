package io.github.luminion.velo.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.annotation.CostLog;
import io.github.luminion.velo.log.annotation.RequestHeadersLog;
import io.github.luminion.velo.log.annotation.ResponseHeadersLog;
import io.github.luminion.velo.log.annotation.composed.InvokeLog;
import io.github.luminion.velo.log.aspect.InvokeLogAspect;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.model.InvocationLogFeature;
import io.github.luminion.velo.log.model.InvocationLogRecord;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.model.LogPolicyLevel;
import io.github.luminion.velo.log.spi.InvocationLogWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 验证控制器日志收集请求头及注解声明时的日志来源控制。
 */
class ControllerHeaderLogTests {
    @Test
    void masksSensitiveHeadersCaseInsensitivelyAndKeepsOthers() throws Throwable {
        VeloProperties properties = new VeloProperties();
        List<InvocationLogRecord> records = new ArrayList<>();
        InvocationLogWriter writer = records::add;
        ControllerLogAspect aspect =
                new ControllerLogAspect(new InvocationLogEngine(properties, String::valueOf, writer));
        MethodInvocation point = mock(MethodInvocation.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/headers");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("x-debug", "yes");
        request.addHeader("Authorization", "secret");
        request.addHeader("cookie", "sid=private");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        try {
            when(point.getThis()).thenReturn(new Endpoint());
            when(point.getArguments()).thenReturn(new Object[0]);
            when(point.getMethod()).thenReturn(Endpoint.class.getDeclaredMethod("handle"));
            when(point.proceed())
                    .thenAnswer(
                            invocation -> {
                                response.addHeader("X-Result", "ok");
                                response.addHeader("Set-Cookie", "token=private");
                                return "done";
                            });
            aspect.invoke(point);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
        assertThat(records)
                .extracting(InvocationLogRecord::getFeature)
                .containsExactly(
                        InvocationLogFeature.ENTRY_ARGS,
                        InvocationLogFeature.REQUEST_HEADERS,
                        InvocationLogFeature.EXIT_RESULT,
                        InvocationLogFeature.RESPONSE_HEADERS,
                        InvocationLogFeature.COST_LOG);
        assertThat(records.get(1).getContent())
                .contains("x-debug", "yes", "Authorization", "cookie", "***")
                .doesNotContain("secret", "sid=private");
        assertThat(records.get(3).getContent())
                .contains("X-Result", "ok", "Set-Cookie", "***")
                .doesNotContain("token=private");
    }

    @Test
    void configuredMaskedHeadersReplaceDefaults() throws Throwable {
        VeloProperties properties = new VeloProperties();
        properties.getLog().setMaskedHeaders(new ArrayList<>(Arrays.asList("X-Token")));
        List<InvocationLogRecord> records = new ArrayList<>();
        ControllerLogAspect aspect =
                new ControllerLogAspect(new InvocationLogEngine(properties, String::valueOf, records::add));
        MethodInvocation point = mock(MethodInvocation.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/headers");
        request.addHeader("x-token", "hidden");
        request.addHeader("Authorization", "visible-now");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, new MockHttpServletResponse()));
        try {
            when(point.getThis()).thenReturn(new Endpoint());
            when(point.getArguments()).thenReturn(new Object[0]);
            when(point.getMethod()).thenReturn(Endpoint.class.getDeclaredMethod("handle"));
            when(point.proceed()).thenReturn("done");
            aspect.invoke(point);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
        assertThat(records.get(1).getFeature()).isEqualTo(InvocationLogFeature.REQUEST_HEADERS);
        assertThat(records.get(1).getContent())
                .contains("x-token", "***", "Authorization", "visible-now")
                .doesNotContain("hidden");
    }

    @Test
    void explicitAnnotationsKeepControllerContextAndNeverDuplicateOrBypassSourceSwitch() {
        VeloProperties properties = new VeloProperties();
        List<InvocationLogRecord> records = new ArrayList<>();
        InvocationLogEngine engine = new InvocationLogEngine(properties, String::valueOf, records::add);
        AspectJProxyFactory factory = new AspectJProxyFactory(new ProxiedEndpoint());
        factory.addAdvisor(new DefaultPointcutAdvisor(ControllerLogAspect.pointcut(), new ControllerLogAspect(engine)));
        factory.addAdvisor(new DefaultPointcutAdvisor(InvokeLogAspect.pointcut(), new InvokeLogAspect(engine)));
        ProxiedEndpoint endpoint = factory.getProxy();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/profile");
        request.setRemoteAddr("203.0.113.7");
        request.addHeader("X-Debug", "visible");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.addHeader("X-Result", "ok");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        try {
            assertThat(endpoint.handle()).isEqualTo("done");
            assertThat(records).hasSize(5).allSatisfy(record -> {
                assertThat(record.getSource()).isEqualTo(InvocationLogSource.CONTROLLER);
                assertThat(record.getTarget()).isEqualTo("handle()");
                assertThat(record.getContextContent()).isEqualTo("203.0.113.7 GET /profile");
            });
            assertThat(records.get(4).getLevel()).isEqualTo(LogPolicyLevel.DEBUG);
            records.clear();
            properties.getLog().getSources().getController().setEnabled(false);
            endpoint.handle();
            assertThat(records).isEmpty();
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @RestController
    @InvokeLog
    static class ProxiedEndpoint {
        @CostLog(level = LogPolicyLevel.DEBUG)
        @RequestHeadersLog
        @ResponseHeadersLog
        public String handle() { return "done"; }
    }

    static class Endpoint {
        @RequestHeadersLog
        @ResponseHeadersLog
        public String handle() {
            return "done";
        }
    }
}
