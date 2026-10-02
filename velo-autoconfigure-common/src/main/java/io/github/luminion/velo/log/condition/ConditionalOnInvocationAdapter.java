package io.github.luminion.velo.log.condition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Conditional;

/** 日志或 trace 任一功能启用时注册入口适配，避免日志开关影响链路。 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Conditional(InvocationAdapterCondition.class)
public @interface ConditionalOnInvocationAdapter {
  String source();
}
