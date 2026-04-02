package io.github.luminion.velo.test;

import org.springframework.data.redis.core.StringRedisTemplate;

public class TestStringRedisTemplate extends StringRedisTemplate {

    @Override
    public void afterPropertiesSet() {
    }
}
