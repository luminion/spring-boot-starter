package io.github.luminion.starter.feature.log.annotation;

import java.lang.annotation.*;

/**
 * 复合调用日志注解
 * 整合了入参和返回值的记录能力
 *
 * @author luminion
 * @since 1.0.0
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgsLog
@ResultLog
@ErrorLog
public @interface InvokeLog {
}
