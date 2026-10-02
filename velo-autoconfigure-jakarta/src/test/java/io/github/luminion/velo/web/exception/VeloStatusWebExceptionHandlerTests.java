package io.github.luminion.velo.web.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import static org.assertj.core.api.Assertions.assertThat;

class VeloStatusWebExceptionHandlerTests {
    @Test
    void followsSpringStatusAndKeepsUserResponseBody() {
        VeloStatusWebExceptionHandler<String> handler = new VeloStatusWebExceptionHandler<>(
                message -> "failed:" + message, error -> "error:" + error.getClass().getSimpleName());
        org.springframework.http.ResponseEntity<String> response = handler.handle(
                new HttpRequestMethodNotSupportedException("PATCH"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isEqualTo("error:HttpRequestMethodNotSupportedException");
    }

    @Test
    void usesServerErrorForExceptionWithoutSpringStatus() {
        VeloStatusWebExceptionHandler<String> handler = new VeloStatusWebExceptionHandler<>(
                message -> "failed:" + message, error -> "error");
        assertThat(handler.handle(new IllegalStateException("boom")).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
