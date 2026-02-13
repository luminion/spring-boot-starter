package io.github.luminion.starter.ratelimit;

/**
 * 限流处理器 SPI
 *
 * @author luminion
 */
public interface RateLimiter {

    /**
     * 尝试获取访问许可 (令牌)
     *
     * @param key   限流 Key
     * @param rate  速率
     * @param burst 突发量
     * @return true: 允许访问; false: 已被限流
     */
    boolean tryAcquire(String key, double rate, double burst);

}
