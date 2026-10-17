package io.github.luminion.velo.log.annotation.composed;

import io.github.luminion.velo.log.annotation.CostLog;
import io.github.luminion.velo.log.annotation.EntryArgs;
import io.github.luminion.velo.log.annotation.ErrorLog;
import io.github.luminion.velo.log.annotation.ExitArgs;
import io.github.luminion.velo.log.annotation.ExitResult;
import io.github.luminion.velo.log.annotation.RequestHeadersLog;
import io.github.luminion.velo.log.annotation.ResponseHeadersLog;
import io.github.luminion.velo.log.annotation.SlowLog;
import io.github.luminion.velo.log.model.LogPolicyLevel;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 普通调用的默认功能组合；包含完整默认值，与其他业务组合按相同规则解析。
 */
@EntryArgs
@ExitArgs(level = LogPolicyLevel.OFF)
@ExitResult
@CostLog
@SlowLog
@ErrorLog(level = LogPolicyLevel.WARN)
@RequestHeadersLog(level = LogPolicyLevel.OFF)
@ResponseHeadersLog(level = LogPolicyLevel.OFF)
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InvokeLog {}
