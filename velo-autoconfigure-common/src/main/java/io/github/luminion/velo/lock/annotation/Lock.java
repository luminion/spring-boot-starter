package io.github.luminion.velo.lock.annotation;

import java.lang.annotation.*;

/**
 * 分布式锁注解
 *
 * @author luminion
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lock {

    /**
     * 锁的 Key（支持 SpEL 表达式）。
     * <p>
     * 为空时降级为方法级锁（基于 类名#方法名(参数类型...)），表示"该方法全局串行执行"，
     * 适用于无需按参数区分的全局互斥场景。
     * <p>
     * 需要按业务维度加锁时请显式指定，例如 {@code key = "#orderId"}。
     */
    String key() default "";

    /**
     * 等待获取锁的时间，单位为毫秒。
     * <p>
     * 默认不等待，获取不到立即失败。
     */
    long waitTimeout() default 0;

    /**
     * 锁的持有时间（自动释放时间），单位为毫秒。
     * <p>
     * Redis 简单实现和使用正数租约的 Redisson 都会按该值自动释放锁，防止持有者宕机后锁永不释放；
     * 本地 Caffeine / JDK 实现忽略该值，靠方法结束时 unlock 释放。
     * <p>
     * 默认 30 秒，适用于绝大多数 CRUD 操作。在依赖该兜底的 Redis 后端上，执行时间可能超过 lease 的长任务
     * （如批量处理、复杂计算）需显式调大，避免锁提前释放。
     * <p>
     * 特殊值 {@code -1}：请求看门狗式自动续约。仅 Redisson 后端真正支持——锁随业务执行自动续约、结束时释放，
     * 适合耗时不确定的长任务；Redis 简单实现不支持，会降级为固定默认租约并打印告警。除 {@code -1} 外，其余非正值非法。
     */
    long lease() default 30000;

    /**
     * 失败提示信息
     */
    String message() default "系统繁忙，请稍后再试";

}
