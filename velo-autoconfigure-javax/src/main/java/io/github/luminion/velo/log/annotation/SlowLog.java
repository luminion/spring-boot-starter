package io.github.luminion.velo.log.annotation;

import java.lang.annotation.*;

// @Inherited: 类级标注需被子类继承，否则子类新增/重写的方法其声明类不带本注解，@within 匹配不到而漏日志。
@Inherited
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SlowLog {

    /**
     * 慢调用阈值，单位为毫秒。
     */
    long value() default 200;
}
