package io.github.luminion.velo.condition;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证列表属性条件对缺失、空列表和有效值的匹配规则。 */
class OnListPropertyConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void shouldMatchWhenListPropertyHasValues() {
        contextRunner
                .withUserConfiguration(NonEmptyListConfiguration.class)
                .withPropertyValues("demo.values[0]=alpha", "demo.values[1]=beta")
                .run(context -> assertThat(context).hasBean("marker"));
    }

    @Test
    void shouldNotMatchWhenListPropertyMissingByDefault() {
        contextRunner
                .withUserConfiguration(NonEmptyListConfiguration.class)
                .run(context -> assertThat(context).doesNotHaveBean("marker"));
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnListProperty("demo.values")
    static class NonEmptyListConfiguration {

        @org.springframework.context.annotation.Bean
        String marker() {
            return "marker";
        }
    }
}
