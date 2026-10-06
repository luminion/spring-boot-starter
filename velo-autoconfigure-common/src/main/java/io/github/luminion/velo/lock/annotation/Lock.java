package io.github.luminion.velo.lock.annotation;

import org.springframework.core.annotation.AliasFor;
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

    /** key 的简写属性。 */
    @AliasFor("key")
    String value() default "";

    /**
     * 锁的 Key（支持 SpEL 表达式）。
     * <p>
     * 为空时降级为方法级锁（基于 类名#方法名(参数类型...)），表示"该方法全局串行执行"，
     * 适用于无需按参数区分的全局互斥场景。
     * <p>
     * 需要按业务维度加锁时请显式指定，例如 {@code key = "#orderId"}。
     */
    @AliasFor("value")
    String key() default "";

    /**
     * 失败提示信息
     */
    String message() default "系统繁忙，请稍后再试";

}
