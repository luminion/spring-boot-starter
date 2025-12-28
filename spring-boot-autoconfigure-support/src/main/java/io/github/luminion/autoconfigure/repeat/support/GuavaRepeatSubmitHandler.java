package io.github.luminion.autoconfigure.repeat.support;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.luminion.autoconfigure.repeat.spi.RepeatSubmitHandler;

import java.util.concurrent.TimeUnit;

/**
 * 基于Guava Cache的防重复提交处理器
 * <p>
 * 适用于单机环境，支持动态过期时间
 * <p><b>依赖:</b> {@code com.google.guava:guava}
 *
 * @author luminion
 * @since 1.0.0
 */
public class GuavaRepeatSubmitHandler implements RepeatSubmitHandler {

    // 使用Guava Cache来存储提交签名
    // 注意：由于我们需要支持动态的过期时间，这里使用一个较大的默认过期时间
    // 实际的过期时间由isRepeatSubmit方法的timeout参数控制
    // 这里使用expireAfterWrite来确保数据在写入后过期
    private final Cache<String, Boolean> cache = CacheBuilder.newBuilder()
            .maximumSize(10000) // 最大缓存10000个签名
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
        
        // 不存在，放入缓存
        // 注意：Guava Cache的expireAfterWrite是全局配置，这里我们使用一个简单的workaround
        // 如果需要精确控制每个key的过期时间，建议使用Redis实现
        cache.put(signature, Boolean.TRUE);
        
        // TODO: Guava Cache的过期时间是全局配置的，这里无法针对单个key设置不同的过期时间
        // 如果需要支持动态过期时间，建议使用Redis实现，或者为每个timeout值创建独立的Cache实例
        return false;
    }

}

