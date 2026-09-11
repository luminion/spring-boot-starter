package io.github.luminion.velo.mybatisplus;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

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
                    assertThat(context).hasSingleBean(
                            com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor.class);
                    assertThat(context).hasSingleBean(OptimisticLockerInnerInterceptor.class);
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
}
