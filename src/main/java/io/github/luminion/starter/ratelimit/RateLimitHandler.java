package io.github.luminion.starter.ratelimit;

import java.util.concurrent.TimeUnit;

/**
 * 限流处理器 SPI
 *
 * @author luminion
 */
public interface RateLimitHandler {

    /**
     * 尝试获取令牌
     *
     * @param key     限流键
     * @param rate    时间窗口内允许的最大请求数
     * @param timeout 时间窗口大小
     * @param unit    时间窗口单位
     * @return true 表示允许通过，false 表示被限流
     */
    boolean tryAcquire(String key, double rate, long timeout, TimeUnit unit);

}
