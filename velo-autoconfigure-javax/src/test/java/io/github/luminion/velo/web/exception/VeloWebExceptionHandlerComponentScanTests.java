package io.github.luminion.velo.web.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class VeloWebExceptionHandlerComponentScanTests {

    @Test
    void shouldNotBeRegisteredByWideComponentScan() {
        assertThat(VeloWebExceptionHandler.class.isAnnotationPresent(RestControllerAdvice.class)).isFalse();
        assertThat(VeloValidationWebExceptionHandler.class.isAnnotationPresent(RestControllerAdvice.class)).isFalse();

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class, true));

        List<String> candidates = scanner.findCandidateComponents("io.github.luminion.velo.web.exception")
                .stream()
                .map(BeanDefinition::getBeanClassName)
                .collect(Collectors.toList());

        assertThat(candidates).doesNotContain(
                VeloWebExceptionHandler.class.getName(),
                VeloValidationWebExceptionHandler.class.getName());
    }
}
