package io.github.luminion.velo.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Spring Boot 2 自动配置入口可以在真实应用启动流程中加载。
 *
 * @author luminion
 */
class Boot2AutoConfigurationStartupTest {

    /** 确认版本专属自动配置能随真实应用启动并提供 Redis 模板。 */
    @Test
    void shouldStartWithVersionSpecificAutoConfigurations() {
        SpringApplication application = new SpringApplication(TestApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setRegisterShutdownHook(false);

        ConfigurableApplicationContext context = application.run(
                "--spring.main.banner-mode=off",
                "--velo.banner.enabled=false"
        );
        try {
            assertThat(context).isNotNull();
            assertThat(context.containsBean("veloRedisSerializer")).isTrue();
            RedisSerializer<?> serializer = context.getBean(RedisSerializer.class);
            assertThat(context.containsBean("redisTemplate")).isTrue();
            assertThat(context.containsBean("stringRedisTemplate")).isTrue();
            assertThat(context.containsBean("stringObjectRedisTemplate")).isTrue();
            assertThat(context.getBean("redisTemplate", RedisTemplate.class).getValueSerializer())
                    .isSameAs(serializer);
            assertThat(context.getBean("stringObjectRedisTemplate", RedisTemplate.class).getValueSerializer())
                    .isSameAs(serializer);
        } finally {
            context.close();
        }
    }

    /** 提供启动集成测试使用的最小应用配置。 */
    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = "org.redisson.spring.starter.RedissonAutoConfigurationV2")
    static class TestApplication {
    }
}
