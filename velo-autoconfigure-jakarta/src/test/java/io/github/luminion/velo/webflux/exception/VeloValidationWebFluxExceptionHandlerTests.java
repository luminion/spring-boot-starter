package io.github.luminion.velo.webflux.exception;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import jakarta.validation.ConstraintViolationException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class VeloValidationWebFluxExceptionHandlerTests {

    @Test
    void shouldHandleJakartaConstraintViolations() {
        VeloValidationWebFluxExceptionHandler<String> handler =
                new VeloValidationWebFluxExceptionHandler<>(message -> "failed:" + message, error -> "error");

        assertThat(handler.handleConstraintViolationException(new ConstraintViolationException(
                Collections.emptySet()))).isEqualTo("failed:");
    }

    @Test
    void shouldHandleSpringMethodValidationErrors() {
        VeloValidationWebFluxExceptionHandler<String> handler =
                new VeloValidationWebFluxExceptionHandler<>(message -> "failed:" + message, error -> "error");
        HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
        doReturn(Collections.singletonList(new DefaultMessageSourceResolvable(null, "must be positive")))
                .when(exception).getAllErrors();

        assertThat(handler.handleHandlerMethodValidationException(exception))
                .isEqualTo("failed:must be positive");
    }
}
