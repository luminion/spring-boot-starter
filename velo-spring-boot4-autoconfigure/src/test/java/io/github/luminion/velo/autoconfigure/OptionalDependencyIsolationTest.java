package io.github.luminion.velo.autoconfigure;

import io.github.luminion.velo.cache.VeloCacheAutoConfiguration;
import io.github.luminion.velo.mybatisplus.VeloMybatisPlusAutoConfiguration;
import io.github.luminion.velo.redis.VeloRedisAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class OptionalDependencyIsolationTest {

    @Test
    void shouldSkipRedisWhenOptionalLibraryIsMissing() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("org.springframework.data.redis"))
                .withConfiguration(AutoConfigurations.of(VeloRedisAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("redisTemplate");
                });
    }

    @Test
    void shouldSkipMybatisPlusWhenOptionalLibraryIsMissing() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("com.baomidou.mybatisplus"))
                .withConfiguration(AutoConfigurations.of(VeloMybatisPlusAutoConfiguration.class))
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void shouldSkipRedisCacheWhenOptionalLibraryIsMissing() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("org.springframework.data.redis"))
                .withConfiguration(AutoConfigurations.of(VeloCacheAutoConfiguration.class))
                .run(context -> assertThat(context).hasNotFailed());
    }
}
