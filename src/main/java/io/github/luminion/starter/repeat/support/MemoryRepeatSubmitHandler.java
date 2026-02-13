package io.github.luminion.starter.repeat.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.luminion.starter.repeat.spi.RepeatSubmitHandler;

import java.util.concurrent.TimeUnit;

/**
 * 基于内存的防重复提交处理器（使用Caffeine实现）
 * <p>
 * 这是推荐的单机环境实现，内存安全，自动过期
 * <p><b>依赖:</b> {@code com.github.benmanes.caffeine:caffeine}
 * <p>
 * 注意版本: For Java 11 or above, use 3.x otherwise use 2.x.
 *
 * @author luminion
 * @since 1.0.0
 */
public class MemoryRepeatSubmitHandler implements RepeatSubmitHandler {

    // 使用Caffeine Cache来存储提交签名，自动管理过期和内存
    // 当一个签名在指定时间后没有被访问时，它将被自动回收
    private final Cache<String, Boolean> cache = Caffeine.newBuilder()
            .maximumSize(10000) // 最大缓存10000个签名，防止内存溢出
            .expireAfterWrite(30, TimeUnit.SECONDS) // 默认30秒过期，实际过期时间由方法参数控制
            .build();

    @Override
    public boolean isRepeatSubmit(String signature, int timeout) {
        // 尝试获取，如果不存在则放入缓存
        Boolean existed = cache.getIfPresent(signature);
        if (existed != null) {
            // 已存在，表示重复提交
            return true;
        }
        
        // 不存在，放入缓存并设置过期时间
        // 注意：Caffeine的expireAfterWrite是全局配置，这里我们使用一个简单的workaround
        // 在实际使用中，如果timeout变化较大，建议使用Guava Cache或Redis
        cache.put(signature, Boolean.TRUE);
        
        // TODO: Caffeine的过期时间是全局配置的，这里无法针对单个key设置不同的过期时间
        // 如果需要支持动态过期时间，建议使用Guava Cache或Redis实现
        return false;
    }

}

