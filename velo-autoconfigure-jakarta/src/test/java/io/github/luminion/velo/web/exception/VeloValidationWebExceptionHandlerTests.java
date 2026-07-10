package io.github.luminion.velo.web.exception;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VeloValidationWebExceptionHandlerTests {

    @Test
    void shouldHandleSpringMvcMethodValidationErrorsAsFailedResponse() {
        AtomicReference<String> failedMessage = new AtomicReference<>();
        VeloValidationWebExceptionHandler<String> handler = new VeloValidationWebExceptionHandler<>(message -> {
            failedMessage.set(message);
            return message;
        }, error -> "error");
        HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
        ParameterValidationResult result = mock(ParameterValidationResult.class);
        when(result.getResolvableErrors()).thenReturn(Collections.singletonList(
                new DefaultMessageSourceResolvable(null, "must be positive")));
        when(exception.getParameterValidationResults()).thenReturn(Collections.singletonList(result));
        when(exception.getCrossParameterValidationResults()).thenReturn(Collections.emptyList());

        String response = handler.handleHandlerMethodValidationException(exception);

        assertThat(response).isEqualTo("must be positive");
        assertThat(failedMessage.get()).isEqualTo("must be positive");
    }
}
