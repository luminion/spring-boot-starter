package io.github.luminion.velo.test;

import org.springframework.data.redis.core.StringRedisTemplate;

/** 提供用于测试的 StringRedisTemplate 实例。 */
public class TestStringRedisTemplate extends StringRedisTemplate {

    @Override
    public void afterPropertiesSet() {
    }
}
