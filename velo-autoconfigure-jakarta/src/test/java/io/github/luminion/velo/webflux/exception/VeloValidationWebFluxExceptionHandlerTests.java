package io.github.luminion.velo.webflux.exception;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolationException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class VeloValidationWebFluxExceptionHandlerTests {

    @Test
    void shouldHandleJakartaConstraintViolations() {
        VeloValidationWebFluxExceptionHandler<String> handler =
                new VeloValidationWebFluxExceptionHandler<>(message -> "failed:" + message, error -> "error");

        assertThat(handler.handleConstraintViolationException(new ConstraintViolationException(
                Collections.emptySet()))).isEqualTo("failed:");
    }
}
