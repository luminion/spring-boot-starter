package io.github.luminion.velo.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogEngine;
import io.github.luminion.velo.log.InvocationLogFeature;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.annotation.RequestHeadersLog;
import io.github.luminion.velo.log.annotation.ResponseHeadersLog;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
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
                        InvocationLogFeature.RESPONSE_HEADERS);
        assertThat(records.get(1).getPayload())
                .contains("x-debug", "yes")
                .doesNotContain("Authorization", "secret");
        assertThat(records.get(2).getPayload())
                .contains("X-Result", "ok")
                .doesNotContain("X-Private", "private");
    }

    static class Endpoint {
        @RequestHeadersLog(allowlist = {"X-Debug"})
        @ResponseHeadersLog(allowlist = {"x-result"})
        public String handle() {
            return "done";
        }
    }
}
