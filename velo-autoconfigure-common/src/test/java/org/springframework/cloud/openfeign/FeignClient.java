package org.springframework.cloud.openfeign;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
/** 提供测试使用的 Feign 客户端注解定义。 */
public @interface FeignClient {

    String value() default "";

    String name() default "";

    String contextId() default "";
}
