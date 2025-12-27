package io.github.luminion.autoconfigure.repeat.support;

import io.github.luminion.autoconfigure.repeat.spi.RepeatSubmitHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的防重复提交处理器
 * <p>
 * 这是推荐的分布式环境实现，适用于多节点部署
 * <p><b>依赖:</b> {@code org.springframework.boot:spring-boot-starter-data-redis}
 *
 * @author luminion
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class RedisRepeatSubmitHandler implements RepeatSubmitHandler {

    private static final String KEY_PREFIX = "repeat_submit:";
    private final RedisTemplate<Object, Object> redisTemplate;

    @Override
    public boolean isRepeatSubmit(String signature, int timeout) {
        String redisKey = KEY_PREFIX + signature;
        
        // 使用setIfAbsent实现原子性的检查和设置
        // 如果key不存在，设置key并返回true；如果key已存在，返回false
        Boolean success = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", timeout, TimeUnit.SECONDS);
        
        // success为null或false表示重复提交
        return success == null || !success;
    }

}

