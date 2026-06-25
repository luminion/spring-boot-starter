package io.github.luminion.velo.lock.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

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
     * 为空时降级为方法级锁（基于 类名#方法名），表示"该方法全局串行执行"，
     * 适用于无需按参数区分的全局互斥场景。
     * <p>
     * 需要按业务维度加锁时请显式指定，例如 {@code key = "#orderId"}。
     */
    String key() default "";

    /**
     * 等待获取锁的时间。
     * <p>
     * 默认不等待，获取不到立即失败。
     */
    long waitTimeout() default 0;

    /**
     * 锁的持有时间（自动释放时间）。
     * <p>
     * 该语义主要由 Redis / Redisson 这类后端保证；本地 Caffeine / JDK 兜底实现仅保证同 JVM 内互斥，
     * 不提供强一致的自动过期释放能力。
     * <p>
     * 默认 30 秒，适用于绝大多数 CRUD 操作。对于执行时间可能超过 lease 的长任务
     * （如批量处理、复杂计算），请显式设置更大的 lease 值以避免锁提前释放。
     */
    long lease() default 30;

    /**
     * 时间单位
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * 失败提示信息
     */
    String message() default "系统繁忙，请稍后再试";

}
