package io.github.luminion.starter.core.xss;

import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author luminion
 * @since 1.0.0
 */
@RequiredArgsConstructor
@RestControllerAdvice
public class XssAdvice implements Ordered {
    @Override
    public int getOrder() {
        return 1;
    }
}
