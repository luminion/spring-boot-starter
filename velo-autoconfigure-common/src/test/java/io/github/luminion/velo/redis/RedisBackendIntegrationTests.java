package io.github.luminion.velo.redis;

import io.github.luminion.velo.idempotent.support.RedisIdempotentHandler;
import io.github.luminion.velo.lock.support.RedisLockHandler;
import io.github.luminion.velo.ratelimit.support.RedisRateLimitHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用真实 Redis 验证并发后端的脚本和跨处理器行为。
 */
@EnabledIfEnvironmentVariable(named = "VELO_TEST_REDIS_URL", matches = "redis://.+")
class RedisBackendIntegrationTests {

    private static LettuceConnectionFactory connectionFactory;
    private static LettuceConnectionFactory secondConnectionFactory;
    private static StringRedisTemplate stringRedisTemplate;
    private static StringRedisTemplate secondStringRedisTemplate;
    private static RedisTemplate<Object, Object> objectRedisTemplate;
    private static RedisTemplate<Object, Object> secondObjectRedisTemplate;

    @BeforeAll
    static void setUp() {
        URI redisUri = URI.create(System.getenv("VELO_TEST_REDIS_URL"));
        int port = redisUri.getPort() == -1 ? 6379 : redisUri.getPort();
        connectionFactory = newConnectionFactory(redisUri.getHost(), port);
        secondConnectionFactory = newConnectionFactory(redisUri.getHost(), port);
        stringRedisTemplate = newStringRedisTemplate(connectionFactory);
        secondStringRedisTemplate = newStringRedisTemplate(secondConnectionFactory);
        objectRedisTemplate = newObjectRedisTemplate(connectionFactory);
        secondObjectRedisTemplate = newObjectRedisTemplate(secondConnectionFactory);
    }

    @AfterAll
    static void tearDown() {
        if (secondConnectionFactory != null) {
            secondConnectionFactory.destroy();
        }
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void lockRetainsOwnershipUntilOutermostUnlock() {
        String key = uniqueKey("lock");
        RedisLockHandler first = new RedisLockHandler(stringRedisTemplate);
        RedisLockHandler second = new RedisLockHandler(secondStringRedisTemplate);
        try {
            assertThat(first.tryLock(key)).isTrue();
            assertThat(first.tryLock(key)).isTrue();
            assertThat(second.tryLock(key)).isFalse();

            first.unlock(key);
            assertThat(second.tryLock(key)).isFalse();

            first.unlock(key);
            assertThat(second.tryLock(key)).isTrue();
        } finally {
            first.unlock(key);
            first.unlock(key);
            second.unlock(key);
            stringRedisTemplate.delete(key);
        }
    }

    @Test
    void fixedTtlDoesNotRenewAndExpiredOwnerCannotDeleteNewLock() throws InterruptedException {
        String key = uniqueKey("fixed-lock");
        RedisLockHandler first = new RedisLockHandler(stringRedisTemplate, 1);
        RedisLockHandler second = new RedisLockHandler(secondStringRedisTemplate, 1);
        try {
            assertThat(first.tryLock(key)).isTrue();
            long beforeReentry = stringRedisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
            assertThat(first.tryLock(key)).isTrue();
            assertThat(stringRedisTemplate.getExpire(key, TimeUnit.MILLISECONDS)).isLessThanOrEqualTo(beforeReentry);
            first.unlock(key);

            Thread.sleep(1_200);
            assertThat(second.tryLock(key)).isTrue();
            assertThat(first.tryLock(key)).isFalse();
            String newOwner = stringRedisTemplate.opsForValue().get(key);
            first.unlock(key);
            assertThat(stringRedisTemplate.opsForValue().get(key)).isEqualTo(newOwner);
        } finally {
            first.unlock(key);
            second.unlock(key);
            stringRedisTemplate.delete(key);
        }
    }

    @Test
    void idempotencyRecordsExpireAndOnlyOwnerCanRemoveThem() throws InterruptedException {
        String key = uniqueKey("idempotent");
        RedisIdempotentHandler first = new RedisIdempotentHandler(objectRedisTemplate);
        RedisIdempotentHandler second = new RedisIdempotentHandler(secondObjectRedisTemplate);
        try {
            assertThat(first.tryRecord(key, "owner-1", 2_000)).isTrue();
            assertThat(second.tryRecord(key, "owner-2", 2_000)).isFalse();

            second.removeIfMatch(key, "owner-2");
            assertThat(second.tryRecord(key, "owner-2", 2_000)).isFalse();

            first.removeIfMatch(key, "owner-1");
            assertThat(second.tryRecord(key, "owner-2", 150)).isTrue();
            Thread.sleep(250);
            assertThat(first.tryRecord(key, "owner-3", 2_000)).isTrue();
        } finally {
            stringRedisTemplate.delete(key);
        }
    }

    @Test
    void rateLimitSharesOneSecondWindowAcrossClientsWithoutExtendingExpiry() throws InterruptedException {
        String key = uniqueKey("rate-limit");
        String otherKey = uniqueKey("rate-limit-independent");
        RedisRateLimitHandler first = new RedisRateLimitHandler(stringRedisTemplate);
        RedisRateLimitHandler second = new RedisRateLimitHandler(secondStringRedisTemplate);
        try {
            assertThat(first.tryAcquire(key, 2)).isTrue();
            long firstTtl = stringRedisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
            assertThat(firstTtl).isBetween(1L, 1000L);
            Thread.sleep(100);
            assertThat(second.tryAcquire(key, 2)).isTrue();
            assertThat(second.tryAcquire(key, 2)).isFalse();
            assertThat(stringRedisTemplate.getExpire(key, TimeUnit.MILLISECONDS)).isLessThan(firstTtl);
            assertThat(second.tryAcquire(otherKey, 2)).isTrue();
            Thread.sleep(1100);
            assertThat(first.tryAcquire(key, 2)).isTrue();
            assertThat(second.tryAcquire(key, 2)).isTrue();
            assertThat(first.tryAcquire(key, 2)).isFalse();
        } finally {
            stringRedisTemplate.delete(key);
            stringRedisTemplate.delete(otherKey);
        }
    }

    @Test
    void rateLimitAllowsBurstAtFixedWindowBoundary() throws InterruptedException {
        String key = uniqueKey("rate-limit-boundary");
        RedisRateLimitHandler handler = new RedisRateLimitHandler(stringRedisTemplate);
        try {
            assertThat(handler.tryAcquire(key, 1)).isTrue();
            assertThat(handler.tryAcquire(key, 1)).isFalse();
            // 仅缩短测试 key 的当前 TTL 来精确构造边界，避免等待到第 800ms 的时序抖动。
            // 原生的一秒 TTL、后续请求不续期由上一用例独立验证。
            assertThat(stringRedisTemplate.expire(key, 1, TimeUnit.MILLISECONDS)).isTrue();
            Thread.sleep(20);
            assertThat(stringRedisTemplate.hasKey(key)).isFalse();
            // 边界两侧可连续各使用一次额度，QPS=1 不代表任意滚动一秒内最多一次。
            assertThat(handler.tryAcquire(key, 1)).isTrue();
            assertThat(handler.tryAcquire(key, 1)).isFalse();
        } finally {
            stringRedisTemplate.delete(key);
        }
    }

    private static String uniqueKey(String feature) {
        return "velo:integration:" + feature + ':' + UUID.randomUUID();
    }

    private static LettuceConnectionFactory newConnectionFactory(String host, int port) {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(host, port);
        factory.afterPropertiesSet();
        return factory;
    }

    private static StringRedisTemplate newStringRedisTemplate(LettuceConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();
        return template;
    }

    private static RedisTemplate<Object, Object> newObjectRedisTemplate(LettuceConnectionFactory factory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
