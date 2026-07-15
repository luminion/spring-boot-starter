package io.github.luminion.velo.idempotent.support;

import io.github.luminion.velo.idempotent.IdempotentHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的分布式幂等处理器
 *
 * @author luminion
 */
@Slf4j
@RequiredArgsConstructor
public class RedisIdempotentHandler implements IdempotentHandler {

    private final RedisTemplate<Object, Object> redisTemplate;

    /**
     * 仅当 value 等于传入 token 时才删除，保证只清除本次请求写入的记录。
     */
    private static final RedisScript<Long> REMOVE_IF_MATCH_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then\n" +
            "    return redis.call('del', KEYS[1])\n" +
            "else\n" +
            "    return 0\n" +
            "end",
            Long.class);

    @Override
    public boolean tryRecord(String key, String token, long timeout) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, token, timeout, TimeUnit.MILLISECONDS);
        if (success == null) {
            // setIfAbsent 返回 null 说明命令被 Redis 事务/pipeline 排队而非立即执行，此时幂等判定失效。
            // 幂等操作不应包裹在 Redis 事务里，打 WARN 提示误用。
            log.warn("[Velo Starter] Idempotent tryRecord got null from setIfAbsent for key '{}'. " +
                    "This usually means the operation is wrapped in a Redis transaction/pipeline, " +
                    "which defers execution and breaks idempotency. Avoid running idempotent checks inside a Redis transaction.", key);
            return false;
        }
        return success;
    }

    @Override
    public void removeIfMatch(String key, String token) {
        redisTemplate.execute(
                REMOVE_IF_MATCH_SCRIPT,
                Collections.singletonList(key),
                token);
    }

}
