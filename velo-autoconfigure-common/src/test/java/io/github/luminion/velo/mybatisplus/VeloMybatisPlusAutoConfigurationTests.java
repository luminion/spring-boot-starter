package io.github.luminion.velo.mybatisplus;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VeloMybatisPlusAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VeloMybatisPlusAutoConfiguration.class));

    @Test
    void shouldRegisterParserBasedInterceptorsWhenParserModuleIsPresent() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
                    assertThat(context).hasSingleBean(PaginationInnerInterceptor.class);
                    assertThat(context).hasSingleBean(BlockAttackInnerInterceptor.class);
                    assertThat(context).hasSingleBean(OptimisticLockerInnerInterceptor.class);
                    assertThat(context.getBean(MybatisPlusInterceptor.class).getInterceptors())
                            .extracting(Object::getClass)
                            .containsExactly(
                                    OptimisticLockerInnerInterceptor.class,
                                    BlockAttackInnerInterceptor.class,
                                    PaginationInnerInterceptor.class);
                });
    }

    @Test
    void shouldKeepVeloInterceptorsLowerPriorityThanExplicitUserOrder() {
        contextRunner
                .withUserConfiguration(UserInterceptorConfiguration.class)
                .run(context -> {
                    List<InnerInterceptor> interceptors = context.getBean(MybatisPlusInterceptor.class)
                            .getInterceptors();
                    assertThat(interceptors)
                            .extracting(Object::getClass)
                            .containsExactly(
                                    EarlyUserInnerInterceptor.class,
                                    OptimisticLockerInnerInterceptor.class,
                                    BlockAttackInnerInterceptor.class,
                                    PaginationInnerInterceptor.class,
                                    LateUserInnerInterceptor.class);
                });
    }

    @Test
    void shouldSkipParserBasedInterceptorsWhenParserModuleIsAbsent() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(
                        "com.baomidou.mybatisplus.extension.parser",
                        "com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor",
                        "com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
                    assertThat(context).hasSingleBean(OptimisticLockerInnerInterceptor.class);
                    assertThat(context).doesNotHaveBean("paginationInnerInterceptor");
                    assertThat(context).doesNotHaveBean("blockAttackInnerInterceptor");
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class UserInterceptorConfiguration {

        @Bean
        @Order(0)
        EarlyUserInnerInterceptor earlyUserInnerInterceptor() {
            return new EarlyUserInnerInterceptor();
        }

        @Bean
        @Order(Ordered.LOWEST_PRECEDENCE)
        LateUserInnerInterceptor lateUserInnerInterceptor() {
            return new LateUserInnerInterceptor();
        }
    }

    static class EarlyUserInnerInterceptor implements InnerInterceptor {
    }

    static class LateUserInnerInterceptor implements InnerInterceptor {
    }
}
