package io.github.luminion.velo.jackson.annotation;

import java.lang.annotation.*;
import java.util.function.Function;

@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonEncode {

    /** 序列化时使用的字符串转换器类型，必须显式指定。 */
    Class<? extends Function<String, String>> value();

}
