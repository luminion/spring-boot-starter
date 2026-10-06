package io.github.luminion.velo.ratelimit;

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
     * @param qps     每秒请求速率，必须为正整数；额度补充节奏由具体后端决定
     * @return true 表示允许通过，false 表示被限流
     */
    boolean tryAcquire(String key, int qps);

}
