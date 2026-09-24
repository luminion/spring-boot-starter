package io.github.luminion.velo.redis;

import io.github.luminion.velo.idempotent.support.RedisIdempotentHandler;
import io.github.luminion.velo.lock.LockToken;
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
import java.util.concurrent.CompletableFuture;
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
            assertThat(first.lock(key, 0, 5_000)).isTrue();
            assertThat(first.lock(key, 0, 5_000)).isTrue();
            assertThat(second.lock(key, 0, 5_000)).isFalse();

            first.unlock(key);
            assertThat(second.lock(key, 0, 5_000)).isFalse();

            first.unlock(key);
            assertThat(second.lock(key, 0, 5_000)).isTrue();
        } finally {
            first.unlock(key);
            first.unlock(key);
            second.unlock(key);
            first.close();
            second.close();
            stringRedisTemplate.delete(key);
        }
    }

    @Test
    void watchdogLockCanBeReleasedAcrossThreadsByToken() {
        String key = uniqueKey("token-lock");
        RedisLockHandler first = new RedisLockHandler(stringRedisTemplate);
        RedisLockHandler second = new RedisLockHandler(secondStringRedisTemplate);
        LockToken token = null;
        try {
            token = first.lockToken(key, 0, -1).toCompletableFuture().join();
            assertThat(token).isNotNull();
            assertThat(stringRedisTemplate.getExpire(key, TimeUnit.SECONDS)).isPositive();
            assertThat(second.lock(key, 0, 5_000)).isFalse();

            LockToken acquiredToken = token;
            CompletableFuture.runAsync(() -> first.unlockToken(acquiredToken).toCompletableFuture().join()).join();
            assertThat(second.lock(key, 0, 5_000)).isTrue();
        } finally {
            if (token != null) {
                first.unlockToken(token).toCompletableFuture().join();
            }
            second.unlock(key);
            first.close();
            second.close();
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
    void rateLimitIsSharedBetweenHandlers() {
        String key = uniqueKey("rate-limit");
        RedisRateLimitHandler first = new RedisRateLimitHandler(stringRedisTemplate);
        RedisRateLimitHandler second = new RedisRateLimitHandler(secondStringRedisTemplate);
        try {
            assertThat(first.tryAcquire(key, 2.0, 60_000)).isTrue();
            assertThat(second.tryAcquire(key, 2.0, 60_000)).isTrue();
            assertThat(first.tryAcquire(key, 2.0, 60_000)).isFalse();
            assertThat(stringRedisTemplate.getExpire(key, TimeUnit.SECONDS)).isPositive();
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
