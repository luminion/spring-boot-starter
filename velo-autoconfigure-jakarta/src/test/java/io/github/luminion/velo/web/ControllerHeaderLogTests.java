package io.github.luminion.velo.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.core.InvocationLogFeature;
import io.github.luminion.velo.log.core.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.annotation.RequestHeadersLog;
import io.github.luminion.velo.log.annotation.ResponseHeadersLog;
import io.github.luminion.velo.log.annotation.InvokeLog;
import io.github.luminion.velo.log.annotation.CostLog;
import io.github.luminion.velo.log.annotation.LogPolicyLevel;
import io.github.luminion.velo.log.aspect.InvokeLogAspect;
import io.github.luminion.velo.log.core.InvocationLogSource;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ControllerHeaderLogTests {
    @Test
    void capturesAllowlistedHeadersCaseInsensitively() throws Throwable {
        VeloProperties properties = new VeloProperties();
        List<InvocationLogRecord> records = new ArrayList<>();
        InvocationLogWriter writer = records::add;
        ControllerLogAspect aspect =
                new ControllerLogAspect(new InvocationLogEngine(properties, String::valueOf, writer));
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/headers");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("x-debug", "yes");
        request.addHeader("Authorization", "secret");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        try {
            when(point.getSignature()).thenReturn(signature);
            when(point.getTarget()).thenReturn(new Endpoint());
            when(point.getArgs()).thenReturn(new Object[0]);
            when(signature.getMethod()).thenReturn(Endpoint.class.getDeclaredMethod("handle"));
            when(signature.getDeclaringType()).thenReturn(Endpoint.class);
            when(point.proceed())
                    .thenAnswer(
                            invocation -> {
                                response.addHeader("X-Result", "ok");
                                response.addHeader("X-Private", "private");
                                return "done";
                            });
            aspect.logControllerInvocation(point);
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
                .contains("x-debug", "yes")
                .doesNotContain("Authorization", "secret");
        assertThat(records.get(3).getContent())
                .contains("X-Result", "ok")
                .doesNotContain("X-Private", "private");
    }

    @Test
    void explicitAnnotationsKeepControllerContextAndNeverDuplicateOrBypassSourceSwitch() {
        VeloProperties properties = new VeloProperties();
        properties.getLog().getSources().getAnnotation().getEntryArgs().setEnabled(false);
        List<InvocationLogRecord> records = new ArrayList<>();
        InvocationLogEngine engine = new InvocationLogEngine(properties, String::valueOf, records::add);
        AspectJProxyFactory factory = new AspectJProxyFactory(new ProxiedEndpoint());
        factory.addAspect(new ControllerLogAspect(engine));
        factory.addAdvisor(new InvokeLogAspect(engine));
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
                assertThat(record.getTarget()).isEqualTo("203.0.113.7 GET /profile");
            });
            assertThat(records.get(4).getLevel()).isEqualTo(org.springframework.boot.logging.LogLevel.DEBUG);
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
        @RequestHeadersLog(allowlist = "X-Debug")
        @ResponseHeadersLog(allowlist = "X-Result")
        public String handle() { return "done"; }
    }

    static class Endpoint {
        @RequestHeadersLog(allowlist = {"X-Debug"})
        @ResponseHeadersLog(allowlist = {"x-result"})
        public String handle() {
            return "done";
        }
    }
}
