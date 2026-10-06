package io.github.luminion.velo.autoconfigure;

import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.lock.support.RedissonLockHandler;
import io.github.luminion.velo.idempotent.IdempotentHandler;
import io.github.luminion.velo.idempotent.support.RedissonIdempotentHandler;
import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.ratelimit.support.RedissonRateLimitHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.net.URI;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实 Redis 验证版本对应的 Redisson starter 和自动后端选择。
 */
@EnabledIfEnvironmentVariable(named = "VELO_TEST_REDIS_URL", matches = "redis://.+")
class Boot3RedissonIntegrationTest {
    @Test
    void shouldStartMatchingRedissonAndSelectItsBackends() {
        URI uri = URI.create(System.getenv("VELO_TEST_REDIS_URL"));
        int port = uri.getPort() < 0 ? 6379 : uri.getPort();
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Application.class)
                .web(WebApplicationType.NONE)
                .properties("spring.data.redis.host=" + uri.getHost(), "spring.data.redis.port=" + port,
                        "spring.main.banner-mode=off", "velo.banner.enabled=false")
                .run()) {
            LockHandler lock = context.getBean(LockHandler.class);
            IdempotentHandler idempotent = context.getBean(IdempotentHandler.class);
            RateLimitHandler rateLimit = context.getBean(RateLimitHandler.class);
            assertThat(lock).isInstanceOf(RedissonLockHandler.class);
            assertThat(idempotent).isInstanceOf(RedissonIdempotentHandler.class);
            assertThat(rateLimit).isInstanceOf(RedissonRateLimitHandler.class);
            String key = "velo:integration:boot3:" + UUID.randomUUID();
            StringRedisTemplate template = context.getBean(StringRedisTemplate.class);
            try {
                assertThat(template.hasKey(key)).isFalse();
                assertThat(lock.tryLock(key + ":lock")).isTrue();
                lock.unlock(key + ":lock");
                assertThat(idempotent.tryRecord(key, "owner", 5000)).isTrue();
                assertThat(idempotent.tryRecord(key, "other", 5000)).isFalse();
                idempotent.removeIfMatch(key, "owner");
                assertThat(rateLimit.tryAcquire(key + ":rate", 2)).isTrue();
                assertThat(rateLimit.tryAcquire(key + ":rate", 2)).isTrue();
                assertThat(rateLimit.tryAcquire(key + ":rate", 2)).isFalse();
            } finally {
                template.delete(key);
                // Redisson 原生限流器有多个派生 key，由其原生 delete 清理本测试命名空间。
                context.getBean(org.redisson.api.RedissonClient.class).getRateLimiter(key + ":rate").delete();
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Application {
    }
}
