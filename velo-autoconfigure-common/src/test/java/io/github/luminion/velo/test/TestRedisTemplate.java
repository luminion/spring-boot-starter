package io.github.luminion.velo.test;

import org.springframework.data.redis.core.RedisTemplate;

/** 提供用于测试的 RedisTemplate 实例。 */
public class TestRedisTemplate extends RedisTemplate<Object, Object> {

    @Override
    public void afterPropertiesSet() {
    }
}
