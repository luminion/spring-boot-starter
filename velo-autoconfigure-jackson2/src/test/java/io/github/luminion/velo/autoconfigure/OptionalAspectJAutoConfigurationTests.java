package io.github.luminion.velo.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * 在不传递引入 AspectJ 的模块中，覆盖 Java 8 反射读取可选依赖条件的场景。
 */
class OptionalAspectJAutoConfigurationTests {

    @ParameterizedTest
    @ValueSource(strings = {
            "io.github.luminion.velo.log.config.VeloLogAutoConfiguration",
            "io.github.luminion.velo.lock.VeloLockAutoConfiguration",
            "io.github.luminion.velo.ratelimit.VeloRateLimitAutoConfiguration",
            "io.github.luminion.velo.idempotent.VeloIdempotentAutoConfiguration",
            "io.github.luminion.velo.feign.VeloFeignAutoConfiguration"
    })
    void missingAspectJAllowsAnnotationInspectionAndSkipsConfiguration(String configurationName) throws Exception {
        try (FilteredClassLoader classLoader = new FilteredClassLoader("org.aspectj")) {
            Class<?> configuration = Class.forName(configurationName, false, classLoader);
            assertThat(configuration.getDeclaredAnnotations()).isNotEmpty();

            new ApplicationContextRunner()
                    .withClassLoader(classLoader)
                    .withUserConfiguration(configuration)
                    .run(context -> assertThat(context)
                            .hasNotFailed()
                            .doesNotHaveBean(configuration));
        }
    }
}
