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
 * 验证 Spring Boot 3 自动配置入口可以在真实应用启动流程中加载。
 *
 * @author luminion
 */
class Boot3AutoConfigurationStartupTest {

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
            RedisSerializer<?> redisSerializer = context.getBean("redisSerializer", RedisSerializer.class);
            assertThat(context.containsBean("redisTemplate")).isTrue();
            assertThat(context.containsBean("stringRedisTemplate")).isTrue();
            assertThat(context.containsBean("stringObjectRedisTemplate")).isTrue();
            assertThat(context.getBean("redisTemplate", RedisTemplate.class).getValueSerializer())
                    .isSameAs(redisSerializer);
            assertThat(context.getBean("stringObjectRedisTemplate", RedisTemplate.class).getValueSerializer())
                    .isSameAs(redisSerializer);
        } finally {
            context.close();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
