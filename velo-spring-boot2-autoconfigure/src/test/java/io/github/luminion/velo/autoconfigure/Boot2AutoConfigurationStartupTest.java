package io.github.luminion.velo.autoconfigure;

import io.github.luminion.velo.redis.RedisJsonSerializerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Spring Boot 2 自动配置入口可以在真实应用启动流程中加载。
 *
 * @author luminion
 */
class Boot2AutoConfigurationStartupTest {

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
            assertThat(context.containsBean("redisSerializer")).isFalse();
            RedisJsonSerializerFactory factory = context.getBean(RedisJsonSerializerFactory.class);
            Class<?> serializerType = factory.create(Object.class).getClass();
            assertThat(context.containsBean("redisTemplate")).isTrue();
            assertThat(context.containsBean("stringRedisTemplate")).isTrue();
            assertThat(context.containsBean("stringObjectRedisTemplate")).isTrue();
            assertThat(context.getBean("redisTemplate", RedisTemplate.class).getValueSerializer())
                    .isInstanceOf(serializerType);
            assertThat(context.getBean("stringObjectRedisTemplate", RedisTemplate.class).getValueSerializer())
                    .isInstanceOf(serializerType);
        } finally {
            context.close();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = "org.redisson.spring.starter.RedissonAutoConfigurationV2")
    static class TestApplication {
    }
}
