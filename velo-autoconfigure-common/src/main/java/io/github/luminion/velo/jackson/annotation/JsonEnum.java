package io.github.luminion.velo.jackson.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonEnum {

    /** 用于将编码映射为描述的枚举类型，必须显式指定。 */
    Class<? extends Enum<?>> value();

    /** 枚举中的编码字段名称；为空时使用全局枚举映射配置。 */
    String codeField() default "";

    /** 枚举中的描述字段名称；为空时使用全局枚举映射配置。 */
    String nameField() default "";

    /** 派生描述属性的名称后缀；为空时使用全局后缀配置。 */
    String nameSuffix() default "";

}
