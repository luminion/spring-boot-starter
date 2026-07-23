package io.github.luminion.velo.log.annotation;

import java.lang.annotation.*;

/**
 * Enables unified invocation logging for a method or type.
 */
// @Inherited: 类级标注需被子类继承，否则子类新增/重写的方法其声明类不带本注解，@within 匹配不到而漏日志。
@Inherited
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InvokeLog {

    /**
     * Whether to include the arguments' current state when the invocation finishes,
     * including both normal return and exceptional completion.
     *
     * @return {@code true} to include arguments in the finish record
     */
    boolean argsOnFinish() default false;
}
