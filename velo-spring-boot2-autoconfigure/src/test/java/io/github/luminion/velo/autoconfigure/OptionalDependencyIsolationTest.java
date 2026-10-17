package io.github.luminion.velo.autoconfigure;

import io.github.luminion.velo.cache.VeloCacheAutoConfiguration;
import io.github.luminion.velo.mybatisplus.VeloMybatisPlusAutoConfiguration;
import io.github.luminion.velo.redis.VeloRedisAutoConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证可选依赖缺失时对应自动配置能够安全跳过。 */
class OptionalDependencyIsolationTest {

    /** Redis 客户端依赖缺失时不创建 Redis 模板，且上下文正常启动。 */
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

    /** MyBatis-Plus 依赖缺失时上下文仍可正常启动。 */
    @Test
    void shouldSkipMybatisPlusWhenOptionalLibraryIsMissing() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("com.baomidou.mybatisplus"))
                .withConfiguration(AutoConfigurations.of(VeloMybatisPlusAutoConfiguration.class))
                .run(context -> assertThat(context).hasNotFailed());
    }

    /** Redis 客户端依赖缺失时不应用 Redis 缓存增强。 */
    @Test
    void shouldSkipRedisCacheWhenOptionalLibraryIsMissing() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("org.springframework.data.redis"))
                .withConfiguration(AutoConfigurations.of(VeloCacheAutoConfiguration.class))
                .run(context -> assertThat(context).hasNotFailed());
    }
}
