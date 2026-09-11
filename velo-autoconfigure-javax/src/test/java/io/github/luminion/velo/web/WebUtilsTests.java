package io.github.luminion.velo.web;

import io.github.luminion.velo.core.util.WebUtils;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class WebUtilsTests {

    @AfterEach
    void resetRequestAttributes() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenRequestAttributesAreMissing() {
        assertThatThrownBy(WebUtils::getServletRequestAttributes)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Servlet Web 请求上下文");
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenRequestAttributesAreNotServletAttributes() {
        RequestContextHolder.setRequestAttributes(mock(RequestAttributes.class));

        assertThatThrownBy(WebUtils::getServletRequestAttributes)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Servlet Web 请求上下文");
    }

    @Test
    void shouldReturnServletRequestAttributesForServletRequest() {
        HttpServletRequest request = new MockHttpServletRequest();
        ServletRequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);

        assertThat(WebUtils.getServletRequestAttributes()).isSameAs(attributes);
    }
}
