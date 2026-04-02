package io.github.luminion.velo.test;

import org.springframework.data.redis.core.RedisTemplate;

public class TestRedisTemplate extends RedisTemplate<Object, Object> {

    @Override
    public void afterPropertiesSet() {
    }
}
