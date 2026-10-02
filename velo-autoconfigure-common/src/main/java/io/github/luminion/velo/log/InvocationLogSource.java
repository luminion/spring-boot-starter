package io.github.luminion.velo.log;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 调用日志来源及输出标识。 */
@Getter
@RequiredArgsConstructor
public enum InvocationLogSource {
  CONTROLLER("controller"),
  FEIGN("feign"),
  INVOKE("invoke"),
  XXL_JOB("xxl-job"),
  SCHEDULED("scheduled");

  private final String value;
}
